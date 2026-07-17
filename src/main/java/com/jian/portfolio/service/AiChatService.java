package com.jian.portfolio.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jian.portfolio.dto.AiChatResponse;
import com.jian.portfolio.dto.ProjectLink;

import jakarta.annotation.PostConstruct;

@Service
public class AiChatService {

    /*
     * 질문과 관련된 문서를 최대 몇 개까지 검색할지 설정합니다.
     */
    private static final int RETRIEVAL_COUNT = 3;

    /*
     * 검색 점수가 너무 낮은 문서의 링크는 표시하지 않습니다.
     */
    private static final double PROJECT_LINK_SCORE_THRESHOLD = 0.30;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private final String chatModel;
    private final String embeddingModel;

    /*
     * 서버 실행 시 포트폴리오 문서와 임베딩을 저장합니다.
     */
    private final List<PortfolioChunk> portfolioChunks =
            new ArrayList<>();


    public AiChatService(
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.api.base-url}") String baseUrl,
            @Value("${openai.api.chat-model}") String chatModel,
            @Value("${openai.api.embedding-model}") String embeddingModel,
            ObjectMapper objectMapper) {

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(
                        "Authorization",
                        "Bearer " + apiKey
                )
                .defaultHeader(
                        "Content-Type",
                        MediaType.APPLICATION_JSON_VALUE
                )
                .build();

        this.objectMapper = objectMapper;
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
    }


    /* =========================
       RAG 데이터 초기화
    ========================= */

    @PostConstruct
    public void initializeRagData() {

        List<String> chunkTexts =
                loadPortfolioChunks();

        if (chunkTexts.isEmpty()) {
            throw new IllegalStateException(
                    "포트폴리오 RAG 문서가 비어 있습니다."
            );
        }

        List<List<Double>> embeddings =
                createEmbeddings(chunkTexts);

        if (chunkTexts.size() != embeddings.size()) {
            throw new IllegalStateException(
                    "포트폴리오 문서와 임베딩 개수가 일치하지 않습니다."
            );
        }

        portfolioChunks.clear();

        for (int index = 0;
             index < chunkTexts.size();
             index++) {

            portfolioChunks.add(
                    new PortfolioChunk(
                            chunkTexts.get(index),
                            embeddings.get(index)
                    )
            );
        }

        System.out.println(
                "RAG 포트폴리오 문서 준비 완료: "
                + portfolioChunks.size()
                + "개 문서"
        );
    }


    /* =========================
       AI 질문 처리
    ========================= */

    public AiChatResponse ask(String message) {

        if (
            message == null ||
            message.trim().isEmpty()
        ) {
            throw new IllegalArgumentException(
                    "질문을 입력해주세요."
            );
        }

        String question =
                message.trim();

        /*
         * 사용자의 질문을 임베딩합니다.
         */
        List<Double> questionEmbedding =
                createEmbedding(question);

        /*
         * 질문과 가장 유사한 포트폴리오 문서를 검색합니다.
         */
        List<RetrievedChunk> retrievedChunks =
                retrieveRelevantChunks(
                        questionEmbedding
                );

        /*
         * 검색된 문서를 하나의 문자열로 합칩니다.
         */
        String retrievedContext =
                retrievedChunks.stream()
                        .map(RetrievedChunk::text)
                        .collect(
                                Collectors.joining(
                                        "\n\n"
                                )
                        );

        String instructions = """
                당신은 백엔드 개발자 최지안의 포트폴리오 상담원입니다.

                반드시 아래의 '검색된 포트폴리오 자료'만 근거로 답변하세요.

                반드시 지켜야 할 규칙:
                1. 자료에 없는 경력, 기술, 자격증, 수상 내역과 수치를 만들지 마세요.
                2. 확인되지 않은 내용을 추측하지 마세요.
                3. 확인할 수 없는 질문에는
                   '현재 포트폴리오 자료에서 확인할 수 없습니다.'
                   라고 답하세요.
                4. 한국어로 답하세요.
                5. 채용 담당자가 읽기 쉽게 핵심 내용을 먼저 작성하세요.
                6. 답변은 일반적으로 3~6문장으로 작성하세요.
                7. 답변 마지막에는 근거가 된 항목을
                   '근거: 항목명' 형식으로 표시하세요.
                8. 검색된 자료와 관련 없는 내용을 추가하지 마세요.

                검색된 포트폴리오 자료:
                """
                + retrievedContext;

        Map<String, Object> requestBody =
                Map.of(
                        "model", chatModel,
                        "instructions", instructions,
                        "input", question
                );

        String responseBody =
                restClient.post()
                        .uri("/responses")
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

        String answer =
                extractAnswer(responseBody);

        /*
         * 검색된 문서에 해당하는 프로젝트 상세 페이지 링크를 만듭니다.
         */
        List<ProjectLink> relatedProjects =
                createRelatedProjectLinks(
                        retrievedChunks
                );

        return new AiChatResponse(
                answer,
                relatedProjects
        );
    }


    /* =========================
       관련 문서 검색
    ========================= */

    private List<RetrievedChunk> retrieveRelevantChunks(
            List<Double> questionEmbedding) {

        if (portfolioChunks.isEmpty()) {
            throw new IllegalStateException(
                    "RAG 문서가 준비되지 않았습니다."
            );
        }

        return portfolioChunks.stream()
                .map(chunk ->
                        new RetrievedChunk(
                                chunk.text(),
                                cosineSimilarity(
                                        questionEmbedding,
                                        chunk.embedding()
                                )
                        )
                )
                .sorted(
                        Comparator.comparingDouble(
                                RetrievedChunk::score
                        ).reversed()
                )
                .limit(RETRIEVAL_COUNT)
                .toList();
    }


	    /* =========================
	       관련 프로젝트 링크 생성
	    ========================= */
	
	    private List<ProjectLink> createRelatedProjectLinks(
	            List<RetrievedChunk> retrievedChunks) {
	
	        if (retrievedChunks == null || retrievedChunks.isEmpty()) {
	            return List.of();
	        }
	
	        /*
	         * 가장 관련도가 높은 첫 번째 문서만 사용
	         */
	        RetrievedChunk topChunk = retrievedChunks.get(0);
	
	        if (topChunk.score() < PROJECT_LINK_SCORE_THRESHOLD) {
	            return List.of();
	        }
	
	        String text = topChunk.text();
	
	        if (text.contains("[맛누리 프로젝트]")) {
	            return List.of(
	                    new ProjectLink(
	                            "맛누리",
	                            "/projects/matnuri"
	                    )
	            );
	        }
	
	        if (text.contains("[양동이 프로젝트]")) {
	            return List.of(
	                    new ProjectLink(
	                            "양동이",
	                            "/projects/yangdongi"
	                    )
	            );
	        }
	
	        if (text.contains("[AI 여행 플래너 프로젝트]")) {
	            return List.of(
	                    new ProjectLink(
	                            "AI 여행 플래너",
	                            "/projects/ai-travel-planner"
	                    )
	            );
	        }
	
	        if (text.contains("[뉴스 요약 프로젝트]")) {
	            return List.of(
	                    new ProjectLink(
	                            "뉴스 요약",
	                            "/projects/news-summary"
	                    )
	            );
	        }
	
	        if (text.contains("[자기소개서 감정 분석 프로젝트]")) {
	            return List.of(
	                    new ProjectLink(
	                            "자기소개서 감정 분석",
	                            "/projects/cover-letter-ai"
	                    )
	            );
	        }
	
	        if (text.contains("[사이버 범죄 데이터 대시보드]")) {
	            return List.of(
	                    new ProjectLink(
	                            "사이버 범죄 데이터 대시보드",
	                            "/projects/cyber-crime-dashboard"
	                    )
	            );
	        }
	
	        if (text.contains("[대학교 홈페이지 프로젝트]")) {
	            return List.of(
	                    new ProjectLink(
	                            "대학교 홈페이지",
	                            "/projects/university-web"
	                    )
	            );
	        }
	
	        if (text.contains("[인공지능 리터러시 경진대회]")) {
	            return List.of(
	                    new ProjectLink(
	                            "인공지능 리터러시 경진대회",
	                            "/projects/ai-literacy"
	                    )
	            );
	        }
	
	        if (text.contains("[추천 알고리즘 콘텐츠 PT]")) {
	            return List.of(
	                    new ProjectLink(
	                            "추천 알고리즘 콘텐츠 PT",
	                            "/projects/recommendation-presentation"
	                    )
	            );
	        }
	
	        return List.of();
	    }
	



    /* =========================
       질문 임베딩 생성
    ========================= */

    private List<Double> createEmbedding(
            String input) {

        List<List<Double>> embeddings =
                createEmbeddings(
                        List.of(input)
                );

        if (embeddings.isEmpty()) {
            throw new IllegalStateException(
                    "질문 임베딩을 생성하지 못했습니다."
            );
        }

        return embeddings.get(0);
    }


    /* =========================
       여러 문서 임베딩 생성
    ========================= */

    private List<List<Double>> createEmbeddings(
            List<String> inputs) {

        Map<String, Object> requestBody =
                Map.of(
                        "model", embeddingModel,
                        "input", inputs
                );

        String responseBody =
                restClient.post()
                        .uri("/embeddings")
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

        if (
            responseBody == null ||
            responseBody.isBlank()
        ) {
            throw new IllegalStateException(
                    "임베딩 API 응답이 비어 있습니다."
            );
        }

        try {
            JsonNode root =
                    objectMapper.readTree(
                            responseBody
                    );

            JsonNode data =
                    root.path("data");

            if (!data.isArray()) {
                throw new IllegalStateException(
                        "임베딩 응답 형식이 올바르지 않습니다."
                );
            }

            List<EmbeddingResult> results =
                    new ArrayList<>();

            for (JsonNode item : data) {

                int index =
                        item.path("index").asInt();

                List<Double> vector =
                        new ArrayList<>();

                for (
                    JsonNode number :
                    item.path("embedding")
                ) {
                    vector.add(
                            number.asDouble()
                    );
                }

                results.add(
                        new EmbeddingResult(
                                index,
                                vector
                        )
                );
            }

            results.sort(
                    Comparator.comparingInt(
                            EmbeddingResult::index
                    )
            );

            return results.stream()
                    .map(
                            EmbeddingResult::embedding
                    )
                    .toList();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "임베딩 응답을 처리하지 못했습니다.",
                    e
            );
        }
    }


    /* =========================
       코사인 유사도 계산
    ========================= */

    private double cosineSimilarity(
            List<Double> first,
            List<Double> second) {

        if (
            first == null ||
            second == null ||
            first.size() != second.size()
        ) {
            throw new IllegalArgumentException(
                    "임베딩 벡터 크기가 일치하지 않습니다."
            );
        }

        double dotProduct = 0.0;
        double firstMagnitude = 0.0;
        double secondMagnitude = 0.0;

        for (
            int index = 0;
            index < first.size();
            index++
        ) {
            double firstValue =
                    first.get(index);

            double secondValue =
                    second.get(index);

            dotProduct +=
                    firstValue * secondValue;

            firstMagnitude +=
                    firstValue * firstValue;

            secondMagnitude +=
                    secondValue * secondValue;
        }

        if (
            firstMagnitude == 0.0 ||
            secondMagnitude == 0.0
        ) {
            return 0.0;
        }

        return dotProduct /
                (
                    Math.sqrt(firstMagnitude)
                    *
                    Math.sqrt(secondMagnitude)
                );
    }


    /* =========================
       RAG 문서 불러오기
    ========================= */

    private List<String> loadPortfolioChunks() {

        try {
            ClassPathResource resource =
                    new ClassPathResource(
                            "ai-data/portfolio-context.txt"
                    );

            String document =
                    resource.getContentAsString(
                            StandardCharsets.UTF_8
                    );

            return List.of(
                            document.split(
                                    "\\s*---\\s*"
                            )
                    )
                    .stream()
                    .map(String::trim)
                    .filter(text ->
                            !text.isEmpty()
                    )
                    .toList();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "포트폴리오 RAG 문서를 불러오지 못했습니다.",
                    e
            );
        }
    }
    


    /* =========================
       OpenAI 답변 추출
    ========================= */

    private String extractAnswer(
            String responseBody) {

        if (
            responseBody == null ||
            responseBody.isBlank()
        ) {
            throw new IllegalStateException(
                    "AI API 응답이 비어 있습니다."
            );
        }

        try {
            JsonNode root =
                    objectMapper.readTree(
                            responseBody
                    );

            JsonNode output =
                    root.path("output");

            for (JsonNode outputItem : output) {

                JsonNode content =
                        outputItem.path("content");

                for (JsonNode contentItem : content) {

                    if (
                        "output_text".equals(
                                contentItem
                                        .path("type")
                                        .asText()
                        )
                    ) {
                        String answer =
                                contentItem
                                        .path("text")
                                        .asText();

                        if (
                            answer != null &&
                            !answer.isBlank()
                        ) {
                            return answer;
                        }
                    }
                }
            }

            throw new IllegalStateException(
                    "AI 답변 내용을 찾을 수 없습니다."
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "AI 응답을 처리하지 못했습니다.",
                    e
            );
        }
    }


    /* =========================
       내부 데이터 객체
    ========================= */

    private record PortfolioChunk(
            String text,
            List<Double> embedding) {
    }

    private record RetrievedChunk(
            String text,
            double score) {
    }

    private record EmbeddingResult(
            int index,
            List<Double> embedding) {
    }
}
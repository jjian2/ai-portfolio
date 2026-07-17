document.addEventListener(
    "DOMContentLoaded",
    function () {

        const header =
            document.getElementById("header");

        const mobileMenuButton =
            document.getElementById(
                "mobileMenuButton"
            );

        const mobileNav =
            document.getElementById(
                "mobileNav"
            );

        const chatbot =
            document.getElementById(
                "chatbot"
            );

        const closeChatButton =
            document.getElementById(
                "closeChatButton"
            );

        const chatForm =
            document.getElementById(
                "chatForm"
            );

        const chatInput =
            document.getElementById(
                "chatInput"
            );

        const chatMessages =
            document.getElementById(
                "chatMessages"
            );

        const openChatButtons =
            document.querySelectorAll(
                ".open-chat-button"
            );

        const suggestionButtons =
            document.querySelectorAll(
                ".chatbot-suggestions button"
            );

        const mobileNavLinks =
            document.querySelectorAll(
                ".mobile-nav a"
            );


        /* =========================
           Header
        ========================= */

        function updateHeader() {

            if (!header) {
                return;
            }

            if (window.scrollY > 20) {
                header.classList.add(
                    "scrolled"
                );
            } else {
                header.classList.remove(
                    "scrolled"
                );
            }
        }


        /* =========================
           Mobile Menu
        ========================= */

        function toggleMobileMenu() {

            if (
                !mobileNav ||
                !mobileMenuButton
            ) {
                return;
            }

            const isOpen =
                mobileNav.classList.toggle(
                    "open"
                );

            mobileMenuButton.classList.toggle(
                "active",
                isOpen
            );

            document.body.classList.toggle(
                "menu-open",
                isOpen
            );
        }


        function closeMobileMenu() {

            if (mobileNav) {
                mobileNav.classList.remove(
                    "open"
                );
            }

            if (mobileMenuButton) {
                mobileMenuButton.classList.remove(
                    "active"
                );
            }

            document.body.classList.remove(
                "menu-open"
            );
        }


        /* =========================
           Chatbot Open / Close
        ========================= */

        function openChatbot(question) {

            if (!chatbot) {
                return;
            }

            chatbot.classList.add("open");

            if (
                chatInput &&
                typeof question === "string" &&
                question.trim() !== ""
            ) {
                chatInput.value =
                    question;
            }

            setTimeout(
                function () {

                    if (chatInput) {
                        chatInput.focus();
                    }
                },
                100
            );
        }


        function closeChatbot() {

            if (!chatbot) {
                return;
            }

            chatbot.classList.remove(
                "open"
            );
        }


        /* =========================
           Message UI
        ========================= */

        function createMessageElement(
                message,
                type) {

            const messageElement =
                document.createElement(
                    "div"
                );

            const textElement =
                document.createElement(
                    "p"
                );

            const infoElement =
                document.createElement(
                    "small"
                );

            messageElement.classList.add(
                "chat-message"
            );

            if (type === "user") {

                messageElement.classList.add(
                    "user-message"
                );

                infoElement.textContent =
                    "방문자";

            } else {

                messageElement.classList.add(
                    "assistant-message"
                );

                infoElement.textContent =
                    "JIAN AI";
            }

            /*
             * textContent를 사용하므로
             * 사용자 HTML 코드가 실행되지 않습니다.
             */
            textElement.textContent =
                message;

            messageElement.appendChild(
                textElement
            );

            messageElement.appendChild(
                infoElement
            );

            return messageElement;
        }


        function addMessage(
                message,
                type) {

            if (!chatMessages) {
                return null;
            }

            const messageElement =
                createMessageElement(
                    message,
                    type
                );

            chatMessages.appendChild(
                messageElement
            );

            scrollChatToBottom();

            return messageElement;
        }


        function addLoadingMessage() {

            const loadingMessage =
                addMessage(
                    "답변을 작성하고 있습니다...",
                    "assistant"
                );

            if (loadingMessage) {
                loadingMessage.classList.add(
                    "chatbot-loading"
                );
            }

            return loadingMessage;
        }


        function scrollChatToBottom() {

            if (!chatMessages) {
                return;
            }

            chatMessages.scrollTop =
                chatMessages.scrollHeight;
        }


        /* =========================
           관련 프로젝트 링크
        ========================= */

        function addRelatedProjectLinks(
                projects) {

            if (
                !chatMessages ||
                !Array.isArray(projects) ||
                projects.length === 0
            ) {
                return;
            }

            const container =
                document.createElement(
                    "div"
                );

            container.className =
                "chat-related-projects";

            const title =
                document.createElement(
                    "span"
                );

            title.className =
                "chat-related-title";

            title.textContent =
                "관련 프로젝트";

            const linkContainer =
                document.createElement(
                    "div"
                );

            linkContainer.className =
                "chat-related-links";

            projects.forEach(
                function (project) {

                    if (
                        !project ||
                        !project.title ||
                        !project.url
                    ) {
                        return;
                    }

                    const link =
                        document.createElement(
                            "a"
                        );

                    link.className =
                        "chat-related-link";

                    link.href =
                        project.url;

					link.textContent =
						project.title + " →";

                    /*
                     * 같은 창에서 프로젝트 상세 페이지로 이동합니다.
                     */
                    linkContainer.appendChild(
                        link
                    );
                }
            );

            if (
                linkContainer.children
                    .length === 0
            ) {
                return;
            }

            container.appendChild(
                title
            );

            container.appendChild(
                linkContainer
            );

            chatMessages.appendChild(
                container
            );

            scrollChatToBottom();
        }


        /* =========================
           Spring Boot AI API 호출
        ========================= */

        async function requestAiAnswer(
                question) {

            const response =
                await fetch(
                    "/api/ai/chat",
                    {
                        method: "POST",

                        headers: {
                            "Content-Type":
                                "application/json"
                        },

                        body: JSON.stringify({
                            message: question
                        })
                    }
                );

            let data;

            try {
                data =
                    await response.json();

            } catch (error) {

                throw new Error(
                    "서버 응답을 읽지 못했습니다."
                );
            }

            if (!response.ok) {

                throw new Error(
                    data.answer ||
                    "AI 답변을 불러오지 못했습니다."
                );
            }

            if (!data.answer) {

                throw new Error(
                    "AI 답변 내용이 비어 있습니다."
                );
            }

            return data;
        }


        /* =========================
           질문 전송
        ========================= */

        async function submitQuestion(
                question) {

            const trimmedQuestion =
                question.trim();

            if (!trimmedQuestion) {
                return;
            }

            addMessage(
                trimmedQuestion,
                "user"
            );

            if (chatInput) {
                chatInput.value = "";
                chatInput.disabled = true;
            }

            const loadingMessage =
                addLoadingMessage();

            try {

                const data =
                    await requestAiAnswer(
                        trimmedQuestion
                    );

                if (loadingMessage) {
                    loadingMessage.remove();
                }

                addMessage(
                    data.answer,
                    "assistant"
                );

                addRelatedProjectLinks(
                    data.relatedProjects
                );

            } catch (error) {

                if (loadingMessage) {
                    loadingMessage.remove();
                }

                console.error(
                    "AI 상담원 오류:",
                    error
                );

                addMessage(
                    error.message ||
                    "AI 답변을 생성하는 중 오류가 발생했습니다.",
                    "assistant"
                );

            } finally {

                if (chatInput) {
                    chatInput.disabled = false;
                    chatInput.focus();
                }
            }
        }


        /* =========================
           Event Listeners
        ========================= */

        window.addEventListener(
            "scroll",
            updateHeader
        );

        updateHeader();


        if (mobileMenuButton) {

            mobileMenuButton.addEventListener(
                "click",
                toggleMobileMenu
            );
        }


        mobileNavLinks.forEach(
            function (link) {

                link.addEventListener(
                    "click",
                    closeMobileMenu
                );
            }
        );


        openChatButtons.forEach(
            function (button) {

                button.addEventListener(
                    "click",
                    function () {

                        const question =
                            button.dataset
                                .question || "";

                        openChatbot(
                            question
                        );

                        closeMobileMenu();
                    }
                );
            }
        );


		if (closeChatButton) {
		    closeChatButton.addEventListener(
		        "click",
		        function (event) {
		            event.preventDefault();
		            event.stopPropagation();
		            closeChatbot();
		        }
		    );
		}


        suggestionButtons.forEach(
            function (button) {

                button.addEventListener(
                    "click",
                    function () {

                        const question =
                            button.dataset
                                .question ||
                            button.textContent
                                .replace("→", "")
                                .trim();

                        submitQuestion(
                            question
                        );
                    }
                );
            }
        );


        if (
            chatForm &&
            chatInput
        ) {
            chatForm.addEventListener(
                "submit",
                function (event) {

                    event.preventDefault();

                    submitQuestion(
                        chatInput.value
                    );
                }
            );
        }


        document.addEventListener(
            "keydown",
            function (event) {

                if (event.key === "Escape") {
                    closeChatbot();
                    closeMobileMenu();
                }
            }
        );

    }
);
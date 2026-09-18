/* ===========================================================================
   AI Chat Module — Interactive Gemini AI Wellness Assistant Drawer
   =========================================================================== */

const AIChatManager = {
  activeConversationId: null,

  init() {
    const chatFab = document.getElementById('chatFab');
    const chatDrawer = document.getElementById('chatDrawer');
    const chatCloseBtn = document.getElementById('chatCloseBtn');
    const chatForm = document.getElementById('chatForm');
    const chatInput = document.getElementById('chatInput');

    if (chatFab && chatDrawer) {
      chatFab.addEventListener('click', () => {
        chatDrawer.classList.toggle('open');
      });
    }

    if (chatCloseBtn && chatDrawer) {
      chatCloseBtn.addEventListener('click', () => {
        chatDrawer.classList.remove('open');
      });
    }

    if (chatForm && chatInput) {
      chatForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const prompt = chatInput.value.trim();
        if (!prompt) return;

        // Render user message immediately
        this.appendMessage('user', prompt);
        chatInput.value = '';

        // Show thinking indicator
        const thinkingId = this.appendMessage('ai', '✨ Thinking...');

        try {
          const res = await ApiClient.sendAIChat(prompt, this.activeConversationId);
          if (res.conversationId) {
            this.activeConversationId = res.conversationId;
          }
          
          // Replace thinking with real response
          this.updateMessage(thinkingId, res.content || 'AI response completed.');
        } catch (err) {
          this.updateMessage(thinkingId, `⚠️ Error: ${err.message}`);
        }
      });
    }
  },

  appendMessage(sender, text) {
    const chatBody = document.getElementById('chatBody');
    if (!chatBody) return;

    const msgDiv = document.createElement('div');
    const msgId = `msg_${Date.now()}`;
    msgDiv.id = msgId;
    msgDiv.className = `chat-msg ${sender}`;
    msgDiv.innerText = text;

    chatBody.appendChild(msgDiv);
    chatBody.scrollTop = chatBody.scrollHeight;
    return msgId;
  },

  updateMessage(msgId, newText) {
    const msgDiv = document.getElementById(msgId);
    if (msgDiv) {
      msgDiv.innerText = newText;
      const chatBody = document.getElementById('chatBody');
      if (chatBody) chatBody.scrollTop = chatBody.scrollHeight;
    }
  }
};

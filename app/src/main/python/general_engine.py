"""
OmniAgent — General Context Handler
Provides fallback analysis for general input that doesn't match specialized modules.
"""

import json
from datetime import datetime

class GeneralEngine:
    def analyze(self, input_text, history=None):
        input_lower = input_text.lower().strip()
        words = input_lower.split()
        
        import re
        
        is_greeting = bool(re.search(r'\b(hi|hello|hey|greetings|morning|afternoon)\b', input_lower))
        
        # Detection rules for offline simulated conversation
        if is_greeting:
            summary = "Hello! I am OmniAgent, your offline AI. How can I assist you today?"
            confidence = 1.0
        elif re.search(r'\b(who are you|what are you)\b', input_lower):
            summary = "I am OmniAgent, a fully offline AI operating system designed to process data securely without internet access."
            confidence = 0.9
        elif re.search(r'\b(how are you)\b', input_lower):
            summary = "I'm functioning perfectly within my local offline environment. What would you like to build or analyze today?"
            confidence = 0.9
        elif re.search(r'\b(help|what can you do)\b', input_lower):
            summary = "I can analyze resumes, perform security and code audits, and evaluate business ideas—all completely offline for your privacy. Just ask!"
            confidence = 0.9
        
        # Conversational fallback rules
        elif match := re.search(r'(what is|what are) (.*)', input_lower):
            topic = match.group(2).replace('?', '').strip()
            summary = f"That's an interesting question about '{topic}'. Since I operate entirely offline for maximum privacy, I don't browse the web for real-time definitions. However, if it relates to coding or system architecture, feel free to paste the code and I'll analyze it for you!"
            confidence = 0.8
        elif match := re.search(r'(how to|how do i) (.*)', input_lower):
            topic = match.group(2).replace('?', '').strip()
            summary = f"To {topic}, there are usually specific technical steps involved. As an offline system, I specialize in analyzing provided code, logic, or resumes to give exact, secure answers. Could you provide the specific code or document you are working on?"
            confidence = 0.8
        elif match := re.search(r'(can you|will you) (.*)', input_lower):
            topic = match.group(2).replace('?', '').strip()
            summary = f"While I operate completely offline, I am fully equipped to help if {topic} pertains to code review, resume ATS scoring, or security analysis. Just paste the details here!"
            confidence = 0.8
        elif input_lower.endswith('?'):
            summary = "That's a great question. Because I am an offline AI designed for security and privacy, my primary capabilities are system analysis, code review, and data structuring. How can I apply these tools to your current project?"
            confidence = 0.7
        else:
            summary = f"I hear you. You mentioned '{input_text}'. As your offline OmniAgent, I'm fully active and ready to process your advanced code, resumes, or security operations securely on this device. Just let me know what you need analyzed."
            confidence = 0.6

        reasoning_list = [
            "Input processed through General Context Engine",
            "Greeting detected: {}".format(is_greeting),
            "Word count: {}".format(len(words))
        ]
        if history:
            reasoning_list.append("Context awareness enabled (History provided)")

        report = {
            "module_name": "General Context Handler",
            "confidence_score": confidence,
            "reasoning": reasoning_list,
            "structured_analysis": {
                "word_count": len(words),
                "summary": summary,
                "sentiment_hints": "Helpful/Positive" if is_greeting else "Informational",
                "suggestions": [
                    "Ask: 'Analyze this Python script for bugs'",
                    "Ask: 'Check this SQL query for vulnerabilities'",
                    "Ask: 'Score my resume for a developer role'",
                    "Ask: 'SWOT analysis for an AI Startup idea'"
                ]
            },
            "risk_score": 0.0,
            "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        }
        return json.dumps(report, indent=2)

_engine = None
def get_engine():
    global _engine
    if _engine is None:
        _engine = GeneralEngine()
    return _engine

def analyze_general(input_text, history=None):
    return get_engine().analyze(input_text, history)

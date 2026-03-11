"""
OmniAgent AI Kernel — Central Intent Classifier & Task Router
Uses TF-IDF vectorization + Cosine Similarity for fully offline intent detection.
No external libraries required — pure Python implementation.
"""

import math
import json
import re
from collections import Counter
from datetime import datetime


class TFIDFVectorizer:
    """Pure Python TF-IDF Vectorizer — no sklearn needed."""

    def __init__(self):
        self.vocabulary = {}
        self.idf_values = {}
        self.documents = []

    def _tokenize(self, text):
        """Tokenize text into lowercase words."""
        text = text.lower()
        tokens = re.findall(r'\b[a-z][a-z0-9_]+\b', text)
        return tokens

    def _compute_tf(self, tokens):
        """Compute term frequency for a document."""
        tf = Counter(tokens)
        total = len(tokens)
        if total == 0:
            return {}
        return {t: count / total for t, count in tf.items()}

    def _compute_idf(self, documents_tokens):
        """Compute inverse document frequency across all documents."""
        n_docs = len(documents_tokens)
        all_terms = set()
        for tokens in documents_tokens:
            all_terms.update(set(tokens))

        idf = {}
        for term in all_terms:
            doc_count = sum(1 for tokens in documents_tokens if term in tokens)
            idf[term] = math.log((n_docs + 1) / (doc_count + 1)) + 1
        return idf

    def fit(self, documents):
        """Fit the vectorizer on a list of document strings."""
        self.documents = documents
        all_tokens = [self._tokenize(doc) for doc in documents]

        # Build vocabulary
        vocab = set()
        for tokens in all_tokens:
            vocab.update(tokens)
        self.vocabulary = {term: idx for idx, term in enumerate(sorted(vocab))}

        # Compute IDF
        self.idf_values = self._compute_idf(all_tokens)
        return self

    def transform(self, texts):
        """Transform texts into TF-IDF vectors."""
        vectors = []
        for text in texts:
            tokens = self._tokenize(text)
            tf = self._compute_tf(tokens)
            vector = {}
            for term, tf_val in tf.items():
                if term in self.idf_values:
                    vector[term] = tf_val * self.idf_values[term]
            vectors.append(vector)
        return vectors

    def fit_transform(self, documents):
        """Fit and transform in one step."""
        self.fit(documents)
        return self.transform(documents)


def cosine_similarity(vec_a, vec_b):
    """Compute cosine similarity between two sparse vectors (dicts)."""
    common_keys = set(vec_a.keys()) & set(vec_b.keys())

    dot_product = sum(vec_a[k] * vec_b[k] for k in common_keys)
    mag_a = math.sqrt(sum(v ** 2 for v in vec_a.values()))
    mag_b = math.sqrt(sum(v ** 2 for v in vec_b.values()))

    if mag_a == 0 or mag_b == 0:
        return 0.0
    return dot_product / (mag_a * mag_b)


# === MODULE DEFINITIONS ===
# Each module has a name, description keywords, and trigger patterns

MODULE_DEFINITIONS = {
    "coding": {
        "name": "Coding Analysis Engine",
        "description": "code programming python java javascript cpp csharp html css function class variable bug debug syntax error compile algorithm data structure loop array list dictionary object method api library framework refactor optimize complexity review analyze parse ast abstract syntax tree if else while for return import include print console log",
        "triggers": [
            "analyze code", "review code", "debug", "find bugs", "code quality",
            "syntax analysis", "refactor", "optimize code", "code review",
            "programming", "algorithm", "function analysis", "parse code",
            "code complexity", "clean code", "code smell", "static analysis",
            "variable tracking", "method extraction", "class design",
            "python script", "java code", "javascript snippet"
        ]
    },
    "cybersecurity": {
        "name": "Cybersecurity Detection Engine",
        "description": "security vulnerability exploit attack sql injection xss cross site scripting malware threat risk assessment penetration testing firewall network scan port authentication authorization encryption hash password brute force phishing payload input validation sanitize select insert update delete from where script alert onerror eval",
        "triggers": [
            "scan for vulnerabilities", "security check", "detect injection",
            "find xss", "sql injection", "penetration test", "security audit",
            "threat detection", "malware check", "vulnerability scan",
            "security analysis", "exploit detection", "input validation",
            "authentication check", "encryption analysis", "password security",
            "network security", "firewall check", "phishing detection",
            "check for exploits"
        ]
    },
    "resume": {
        "name": "Resume ATS Scoring Engine",
        "description": "resume cv curriculum vitae job application hiring ats applicant tracking system skill gap career experience education qualification cover letter interview profile linkedin professional summary objective keyword match score recruit talent workforce",
        "triggers": [
            "analyze resume", "ats score", "resume review", "skill gap",
            "career analysis", "job match", "resume optimization",
            "qualification check", "experience analysis", "resume scoring",
            "cv analysis", "professional profile", "career assessment",
            "job readiness", "skill assessment", "resume feedback",
            "hiring compatibility", "talent assessment"
        ]
    },
    "startup": {
        "name": "Startup Feasibility Engine",
        "description": "startup business idea feasibility market analysis swot strength weakness opportunity threat competitor revenue model investor pitch funding venture capital product market fit scalability growth strategy business plan financial projection customer segment value proposition lean canvas fintech healthtech edtech saas ai machine learning platform startup ecommerce",
        "triggers": [
            "startup idea", "business plan", "market analysis", "swot analysis",
            "feasibility study", "business model", "competitor analysis",
            "revenue projection", "funding strategy", "product market fit",
            "growth strategy", "value proposition", "business feasibility",
            "startup assessment", "market opportunity", "business viability",
            "investor readiness", "lean canvas", "pitch deck", "venture"
        ]
    },
    "general": {
        "name": "General Context Handler",
        "description": "hi hello hey greetings help who are you what is this assistant support thanks thank you goodbye bye clear reset options features exam prepare study test prep tips advice question info information education guidance help student university college school",
        "triggers": [
            "hi", "hello", "hey there", "greetings", "how are you", 
            "what can you do", "help me", "who are you", "tell me about yourself",
            "thanks", "thank you", "goodbye", "bye", "what is this app"
        ]
    }
}


class AIKernel:
    """
    Central AI Kernel — Detects intent, classifies tasks, routes to engines.
    Fully offline, transparent reasoning, no external dependencies.
    """

    def __init__(self):
        self.vectorizer = TFIDFVectorizer()
        self.module_vectors = {}
        self.reasoning_log = []
        self._initialize()

    def _initialize(self):
        """Build TF-IDF model from module definitions."""
        # Combine description + triggers for each module
        corpus = []
        self.module_names = []
        for key, module in MODULE_DEFINITIONS.items():
            text = module["description"] + " " + " ".join(module["triggers"])
            corpus.append(text)
            self.module_names.append(key)

        # Fit vectorizer on module corpus
        vectors = self.vectorizer.fit_transform(corpus)
        for i, key in enumerate(self.module_names):
            self.module_vectors[key] = vectors[i]

        self._log("INIT", "AI Kernel initialized with {} modules".format(len(MODULE_DEFINITIONS)))

    def _log(self, level, message):
        """Add entry to reasoning log."""
        entry = {
            "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "level": level,
            "message": message
        }
        self.reasoning_log.append(entry)

    def classify(self, user_input):
        """
        Classify user input to the best matching module.
        Returns JSON string with module name, confidence, reasoning, and all scores.
        """
        self._log("INPUT", "Received: '{}'".format(user_input[:100]))

        if not user_input or not user_input.strip():
            result = {
                "status": "error",
                "message": "Empty input provided",
                "module": None,
                "confidence": 0.0,
                "reasoning": "No input to classify"
            }
            return json.dumps(result)

        # Vectorize input
        input_vector = self.vectorizer.transform([user_input])[0]
        self._log("VECTORIZE", "Input tokenized into {} features".format(len(input_vector)))

        # Compute similarity against all modules
        scores = {}
        input_lower = user_input.lower().strip()
        for module_key, module_vector in self.module_vectors.items():
            sim = cosine_similarity(input_vector, module_vector)
            
            # Boost score to 1.0 if input exactly matches a defined trigger
            if input_lower in MODULE_DEFINITIONS[module_key]["triggers"]:
                sim = 1.0
                self._log("SCORE BOOST", "Exact trigger match for '{}'".format(module_key))
                
            scores[module_key] = round(sim, 4)
            self._log("SCORE", "{}: {:.4f}".format(module_key, sim))

        # Find best match
        best_module = max(scores, key=scores.get)
        best_score = scores[best_module]

        # Determine confidence level
        if best_score >= 0.3:
            confidence_level = "HIGH"
        elif best_score >= 0.15:
            confidence_level = "MEDIUM"
        elif best_score >= 0.05:
            confidence_level = "LOW"
        else:
            confidence_level = "UNCERTAIN"

        # Build reasoning chain
        sorted_scores = sorted(scores.items(), key=lambda x: x[1], reverse=True)
        reasoning_steps = [
            "Step 1: Tokenized input into {} meaningful terms".format(len(input_vector)),
            "Step 2: Computed TF-IDF vectors for input",
            "Step 3: Calculated cosine similarity against {} modules".format(len(scores)),
            "Step 4: Ranked results — top match: {} ({:.4f})".format(best_module, best_score),
            "Step 5: Confidence assessment: {} (threshold: 0.05 minimum)".format(confidence_level)
        ]

        if best_score < 0.05:
            reasoning_steps.append("Step 6: Score below threshold — no confident classification")
            best_module_name = None
        else:
            reasoning_steps.append("Step 6: Routing to '{}' engine".format(
                MODULE_DEFINITIONS[best_module]["name"]
            ))
            best_module_name = best_module

        self._log("DECISION", "Classified as '{}' with confidence {:.4f} ({})".format(
            best_module, best_score, confidence_level
        ))

        result = {
            "status": "success",
            "module": best_module_name,
            "module_name": MODULE_DEFINITIONS[best_module]["name"] if best_module_name else "Unknown",
            "confidence": best_score,
            "confidence_level": confidence_level,
            "all_scores": scores,
            "ranking": [{"module": m, "score": s} for m, s in sorted_scores],
            "reasoning": reasoning_steps,
            "input_features": len(input_vector),
            "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        }

        return json.dumps(result, indent=2)

    def get_reasoning_log(self):
        """Return full reasoning log as JSON."""
        return json.dumps(self.reasoning_log, indent=2)

    def clear_log(self):
        """Clear reasoning log."""
        self.reasoning_log = []
        self._log("SYSTEM", "Reasoning log cleared")


# === SINGLETON KERNEL INSTANCE ===
_kernel_instance = None

def get_kernel():
    """Get or create the singleton kernel instance."""
    global _kernel_instance
    if _kernel_instance is None:
        _kernel_instance = AIKernel()
    return _kernel_instance

def classify_input(user_input):
    """Entry point for Kotlin/Chaquopy — classify user input."""
    kernel = get_kernel()
    return kernel.classify(user_input)

def get_reasoning_log():
    """Entry point for Kotlin/Chaquopy — get reasoning log."""
    kernel = get_kernel()
    return kernel.get_reasoning_log()

def clear_reasoning_log():
    """Entry point for Kotlin/Chaquopy — clear reasoning log."""
    kernel = get_kernel()
    kernel.clear_log()

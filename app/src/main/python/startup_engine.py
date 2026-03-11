"""
OmniAgent — Startup Feasibility Engine
Performs SWOT analysis, market feasibility, and business model assessment.
Fully offline, returns structured JSON reports.
"""

import re
import json
from datetime import datetime


class StartupEngine:
    """Analyzes startup ideas for feasibility, SWOT, and business viability."""

    # === INDUSTRY KNOWLEDGE BASE ===
    MARKET_CATEGORIES = {
        "fintech": {
            "keywords": ["payment", "banking", "finance", "lending", "insurance", "crypto",
                         "blockchain", "wallet", "transaction", "investment", "trading"],
            "avg_competition": "high",
            "market_trend": "growing",
            "typical_challenges": ["regulatory compliance", "trust building", "security requirements"]
        },
        "healthtech": {
            "keywords": ["health", "medical", "telemedicine", "fitness", "wellness", "diagnosis",
                         "patient", "hospital", "pharmacy", "mental health", "wearable"],
            "avg_competition": "medium",
            "market_trend": "rapidly growing",
            "typical_challenges": ["regulatory approval", "data privacy", "clinical validation"]
        },
        "edtech": {
            "keywords": ["education", "learning", "tutorial", "course", "student", "teacher",
                         "school", "university", "training", "skill", "certification", "e-learning"],
            "avg_competition": "high",
            "market_trend": "growing",
            "typical_challenges": ["content quality", "engagement retention", "monetization"]
        },
        "ecommerce": {
            "keywords": ["shop", "store", "marketplace", "buy", "sell", "product", "delivery",
                         "retail", "inventory", "order", "customer", "shipping"],
            "avg_competition": "very high",
            "market_trend": "mature",
            "typical_challenges": ["logistics", "customer acquisition cost", "margin pressure"]
        },
        "saas": {
            "keywords": ["software", "platform", "tool", "automation", "workflow", "subscription",
                         "dashboard", "analytics", "management", "enterprise", "cloud", "api"],
            "avg_competition": "high",
            "market_trend": "growing",
            "typical_challenges": ["churn reduction", "feature differentiation", "scaling"]
        },
        "ai_ml": {
            "keywords": ["artificial intelligence", "machine learning", "data", "model",
                         "prediction", "automation", "AI", "neural", "deep learning", "nlp",
                         "computer vision", "recommendation"],
            "avg_competition": "medium",
            "market_trend": "rapidly growing",
            "typical_challenges": ["data requirements", "compute costs", "explainability"]
        },
        "social": {
            "keywords": ["social", "community", "network", "connect", "messaging", "chat",
                         "share", "content", "creator", "influencer", "engagement", "feed"],
            "avg_competition": "very high",
            "market_trend": "saturated",
            "typical_challenges": ["user acquisition", "network effects", "content moderation"]
        },
        "sustainability": {
            "keywords": ["green", "sustainable", "environment", "renewable", "recycle", "carbon",
                         "eco", "clean energy", "solar", "waste", "climate"],
            "avg_competition": "low",
            "market_trend": "rapidly growing",
            "typical_challenges": ["cost competitiveness", "behavior change", "measurement"]
        }
    }

    REVENUE_MODELS = {
        "subscription": {"viability": "high", "description": "Monthly/yearly recurring payments"},
        "freemium": {"viability": "high", "description": "Free tier with premium features"},
        "marketplace": {"viability": "medium", "description": "Commission on transactions"},
        "advertising": {"viability": "medium", "description": "Ad-supported with free access"},
        "licensing": {"viability": "high", "description": "License fees for enterprise use"},
        "pay_per_use": {"viability": "medium", "description": "Charge per usage/transaction"},
        "data_monetization": {"viability": "low", "description": "Revenue from data insights (privacy concerns)"},
    }

    def __init__(self):
        self.analysis_log = []

    def _log(self, message):
        self.analysis_log.append({
            "timestamp": datetime.now().strftime("%H:%M:%S"),
            "event": message
        })

    def analyze(self, idea_text):
        """
        Main analysis entry point.
        Accepts startup idea description, returns comprehensive feasibility report.
        """
        self.analysis_log = []
        self._log("Starting startup feasibility analysis")

        text_lower = idea_text.lower()

        # Detect industry/market
        market = self._detect_market(text_lower)
        self._log("Detected market: {}".format(market.get("primary_market", "unknown")))

        # SWOT Analysis
        swot = self._generate_swot(text_lower, market)
        self._log("SWOT analysis completed")

        # Revenue model suggestions
        revenue = self._suggest_revenue_models(text_lower)
        self._log("Revenue models analyzed")

        # Feasibility scoring
        feasibility = self._compute_feasibility(text_lower, market, swot)
        self._log("Feasibility score: {}/100".format(feasibility["total_score"]))

        # Extract key elements from the idea
        elements = self._extract_idea_elements(text_lower)

        # Standardized Output Fields
        final_report = {
            "module_name": "Startup Feasibility Engine",
            "confidence_score": 1.0,
            "reasoning": [
                "Identified primary market: {}".format(market.get("primary_market", "general")),
                "Computed feasibility score: {}/100 ({})".format(feasibility["total_score"], feasibility["grade"]),
                "Decided '{}' based on market readiness and vision clarity".format(
                    self._go_no_go_recommendation(feasibility["total_score"])["decision"]
                )
            ],
            "structured_analysis": {
                "market": market,
                "swot": swot,
                "revenue_models": [r for r in revenue if r["relevance"] != "low"][:3],
                "feasibility": feasibility,
                "go_no_go": self._go_no_go_recommendation(feasibility["total_score"]),
                "next_steps": self._generate_next_steps(feasibility["total_score"], market),
                "recommendations": self._generate_recommendations(market, swot, revenue, feasibility)
            },
            "risk_score": float(100 - feasibility["total_score"]),
            "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        }

        return json.dumps(final_report, indent=2)

    def _detect_market(self, text_lower):
        """Identify the most relevant market category."""
        scores = {}
        for market, info in self.MARKET_CATEGORIES.items():
            score = sum(1 for kw in info["keywords"] if kw in text_lower)
            scores[market] = score

        if max(scores.values()) == 0:
            return {
                "primary_market": "general",
                "confidence": "low",
                "competition_level": "unknown",
                "market_trend": "unknown",
                "all_scores": scores
            }

        best = max(scores, key=scores.get)
        second = sorted(scores.items(), key=lambda x: x[1], reverse=True)

        market_info = self.MARKET_CATEGORIES[best]
        return {
            "primary_market": best,
            "confidence": "high" if scores[best] >= 3 else "medium" if scores[best] >= 2 else "low",
            "competition_level": market_info["avg_competition"],
            "market_trend": market_info["market_trend"],
            "typical_challenges": market_info["typical_challenges"],
            "keyword_matches": scores[best],
            "secondary_market": second[1][0] if len(second) > 1 and second[1][1] > 0 else None,
            "all_scores": scores
        }

    def _generate_swot(self, text_lower, market):
        """Generate SWOT analysis based on idea and market."""
        strengths = []
        weaknesses = []
        opportunities = []
        threats = []

        # Strengths detection
        if any(w in text_lower for w in ["unique", "first", "novel", "innovative", "patent"]):
            strengths.append("Innovative/unique approach detected")
        if any(w in text_lower for w in ["experience", "expertise", "years", "background"]):
            strengths.append("Domain expertise indicated")
        if any(w in text_lower for w in ["team", "co-founder", "partner", "advisor"]):
            strengths.append("Team/partnership elements present")
        if any(w in text_lower for w in ["ai", "machine learning", "automation", "technology"]):
            strengths.append("Technology-driven competitive advantage")
        if any(w in text_lower for w in ["local", "community", "regional"]):
            strengths.append("Local market understanding")
        if not strengths:
            strengths.append("Idea exists — execution is everything")

        # Weaknesses detection
        if not any(w in text_lower for w in ["revenue", "monetize", "profit", "business model"]):
            weaknesses.append("No clear revenue model mentioned")
        if not any(w in text_lower for w in ["team", "co-founder", "hire"]):
            weaknesses.append("Team composition not addressed")
        if not any(w in text_lower for w in ["customer", "user", "target", "audience"]):
            weaknesses.append("Target customer not clearly defined")
        if not any(w in text_lower for w in ["funding", "investment", "bootstrap", "capital"]):
            weaknesses.append("Funding strategy not mentioned")

        # Opportunities
        market_trend = market.get("market_trend", "unknown")
        if market_trend in ["growing", "rapidly growing"]:
            opportunities.append("Market is {} — favorable timing".format(market_trend))
        opportunities.append("Mobile-first approach can capture digital-native users")
        if any(w in text_lower for w in ["global", "international", "worldwide"]):
            opportunities.append("Global expansion potential identified")
        if any(w in text_lower for w in ["ai", "automation"]):
            opportunities.append("AI/automation trend alignment increases investor appeal")
        opportunities.append("Potential for strategic partnerships in the space")

        # Threats
        competition = market.get("competition_level", "unknown")
        if competition in ["high", "very high"]:
            threats.append("High competition in {} space".format(market.get("primary_market", "this")))
        threats.append("Established players may copy the idea quickly")
        if market.get("typical_challenges"):
            for challenge in market["typical_challenges"][:2]:
                threats.append("Industry challenge: {}".format(challenge))
        threats.append("Market conditions and regulations may change")

        return {
            "strengths": strengths,
            "weaknesses": weaknesses,
            "opportunities": opportunities,
            "threats": threats,
            "strength_count": len(strengths),
            "weakness_count": len(weaknesses)
        }

    def _suggest_revenue_models(self, text_lower):
        """Suggest applicable revenue models."""
        suggestions = []
        for model, info in self.REVENUE_MODELS.items():
            relevance = "low"
            if model == "subscription" and any(w in text_lower for w in
                                                ["platform", "service", "tool", "saas", "monthly"]):
                relevance = "high"
            elif model == "freemium" and any(w in text_lower for w in
                                             ["free", "premium", "tier", "basic"]):
                relevance = "high"
            elif model == "marketplace" and any(w in text_lower for w in
                                                ["marketplace", "platform", "connect", "buy", "sell"]):
                relevance = "high"
            elif model == "advertising" and any(w in text_lower for w in
                                                ["content", "social", "media", "free"]):
                relevance = "medium"
            elif model == "licensing" and any(w in text_lower for w in
                                              ["enterprise", "company", "business", "corporate"]):
                relevance = "high"
            elif model == "pay_per_use" and any(w in text_lower for w in
                                                ["usage", "per", "transaction", "api"]):
                relevance = "medium"

            suggestions.append({
                "model": model,
                "description": info["description"],
                "viability": info["viability"],
                "relevance": relevance
            })

        # Sort by relevance
        order = {"high": 0, "medium": 1, "low": 2}
        suggestions.sort(key=lambda x: order.get(x["relevance"], 3))
        return suggestions

    def _compute_feasibility(self, text_lower, market, swot):
        """Compute feasibility score breakdown."""
        scores = {}

        # Market viability (25 pts)
        trend = market.get("market_trend", "unknown")
        if trend == "rapidly growing":
            scores["market_viability"] = 25
        elif trend == "growing":
            scores["market_viability"] = 20
        elif trend == "mature":
            scores["market_viability"] = 12
        elif trend == "saturated":
            scores["market_viability"] = 8
        else:
            scores["market_viability"] = 15

        # Innovation factor (20 pts)
        innovation_words = ["unique", "first", "novel", "innovative", "disrupt", "revolutionize",
                            "patent", "breakthrough", "cutting-edge"]
        innovation_count = sum(1 for w in innovation_words if w in text_lower)
        scores["innovation"] = min(20, innovation_count * 5 + 5)

        # Clarity of vision (20 pts)
        clarity_elements = [
            any(w in text_lower for w in ["problem", "solve", "pain point", "challenge"]),
            any(w in text_lower for w in ["customer", "user", "audience", "target"]),
            any(w in text_lower for w in ["solution", "product", "service", "platform"]),
            any(w in text_lower for w in ["revenue", "monetize", "business model", "profit"]),
        ]
        scores["clarity"] = sum(5 for c in clarity_elements if c)

        # Team strength (15 pts)
        team_words = ["team", "co-founder", "experience", "expert", "years", "background"]
        team_count = sum(1 for w in team_words if w in text_lower)
        scores["team"] = min(15, team_count * 3 + 3)

        # Market readiness (20 pts)
        readiness = 10
        comp = market.get("competition_level", "unknown")
        if comp == "low":
            readiness += 10
        elif comp == "medium":
            readiness += 5
        elif comp in ["high", "very high"]:
            readiness += 0
        scores["market_readiness"] = readiness

        total = sum(scores.values())
        return {
            "total_score": min(100, total),
            "grade": self._feasibility_grade(total),
            "breakdown": scores
        }

    def _feasibility_grade(self, score):
        if score >= 80:
            return "Highly Feasible"
        elif score >= 60:
            return "Feasible"
        elif score >= 40:
            return "Needs Work"
        elif score >= 20:
            return "Risky"
        return "Not Feasible"

    def _go_no_go_recommendation(self, score):
        """Provide go/no-go recommendation."""
        if score >= 70:
            return {
                "decision": "GO",
                "message": "Strong potential — proceed with MVP development",
                "confidence": "high"
            }
        elif score >= 50:
            return {
                "decision": "CONDITIONAL GO",
                "message": "Promising but needs refinement — address weaknesses first",
                "confidence": "medium"
            }
        elif score >= 30:
            return {
                "decision": "HOLD",
                "message": "Significant gaps exist — rethink core assumptions",
                "confidence": "low"
            }
        return {
            "decision": "NO GO",
            "message": "Major viability concerns — pivot or restructure the concept",
            "confidence": "low"
        }

    def _extract_idea_elements(self, text_lower):
        """Extract key business elements from the idea text."""
        elements = {
            "has_problem_statement": any(w in text_lower for w in ["problem", "pain point", "challenge", "issue"]),
            "has_solution": any(w in text_lower for w in ["solution", "solve", "provide", "offer", "build"]),
            "has_target_audience": any(w in text_lower for w in ["customer", "user", "audience", "target", "demographic"]),
            "has_revenue_model": any(w in text_lower for w in ["revenue", "monetize", "charge", "subscription", "pricing"]),
            "has_competitive_advantage": any(w in text_lower for w in ["unique", "better", "faster", "cheaper", "innovative"]),
            "has_scalability": any(w in text_lower for w in ["scale", "grow", "expand", "global", "millions"]),
        }
        elements["completeness"] = "{}%".format(
            round(sum(1 for v in elements.values() if v is True) / 6 * 100)
        )
        return elements

    def _generate_next_steps(self, score, market):
        """Generate actionable next steps."""
        steps = []
        if score >= 60:
            steps = [
                "Validate idea with 10-20 potential users through interviews",
                "Build a Minimum Viable Product (MVP) with core features only",
                "Define 3-month milestone roadmap",
                "Set up basic metrics tracking (user acquisition, retention, engagement)",
                "Research potential funding options (bootstrapping vs investors)"
            ]
        else:
            steps = [
                "Clearly define the problem you're solving and for whom",
                "Research competitors in the {} space".format(market.get("primary_market", "relevant")),
                "Talk to 20+ potential customers before building anything",
                "Define a clear revenue model",
                "Consider a simpler version of the concept to prove feasibility"
            ]
        return steps

    def _generate_recommendations(self, market, swot, revenue, feasibility):
        """Generate strategic recommendations."""
        recs = []

        if feasibility["total_score"] < 50:
            recs.append("Focus on validating core assumptions before development")

        if len(swot["weaknesses"]) > 2:
            recs.append("Address weaknesses: {}".format(
                "; ".join(swot["weaknesses"][:2])
            ))

        high_revenue = [r for r in revenue if r["relevance"] == "high"]
        if high_revenue:
            recs.append("Consider {} revenue model: {}".format(
                high_revenue[0]["model"],
                high_revenue[0]["description"]
            ))
        else:
            recs.append("Define a clear revenue model — this is critical for viability")

        comp = market.get("competition_level", "unknown")
        if comp in ["high", "very high"]:
            recs.append("High competition — focus on a unique differentiator or niche market")

        recs.append("Start lean: build MVP with only essential features first")
        return recs


# === ENTRY POINT FOR CHAQUOPY ===
_engine = None

def get_engine():
    global _engine
    if _engine is None:
        _engine = StartupEngine()
    return _engine

def analyze_startup(idea_text, history=None):
    """Entry point: analyze startup idea and return feasibility report."""
    engine = get_engine()
    return engine.analyze(idea_text)

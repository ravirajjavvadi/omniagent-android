"""
OmniAgent — Resume ATS Scoring Engine
Performs ATS compatibility scoring, skill gap analysis, and professional assessment.
Fully offline, returns structured JSON reports.
"""

import re
import json
from datetime import datetime
from collections import Counter


class ResumeEngine:
    """Analyzes resumes for ATS scoring, skill gaps, and professional readiness."""

    # === SKILL DATABASES ===
    TECH_SKILLS = {
        "programming": ["python", "java", "javascript", "kotlin", "swift", "c++", "c#",
                        "ruby", "go", "rust", "typescript", "php", "scala", "r", "matlab"],
        "web": ["html", "css", "react", "angular", "vue", "nodejs", "express", "django",
                "flask", "spring", "nextjs", "tailwind", "bootstrap", "jquery", "graphql"],
        "data": ["sql", "nosql", "mongodb", "postgresql", "mysql", "redis", "elasticsearch",
                 "pandas", "numpy", "tensorflow", "pytorch", "scikit-learn", "spark", "hadoop"],
        "devops": ["docker", "kubernetes", "aws", "azure", "gcp", "jenkins", "terraform",
                   "ansible", "ci/cd", "nginx", "linux", "git", "github", "gitlab"],
        "mobile": ["android", "ios", "react native", "flutter", "swiftui", "jetpack compose",
                   "xamarin", "cordova", "ionic"],
        "security": ["cybersecurity", "penetration testing", "ethical hacking", "owasp",
                     "encryption", "firewall", "soc", "siem", "vulnerability assessment"]
    }

    SOFT_SKILLS = [
        "leadership", "communication", "teamwork", "problem solving", "critical thinking",
        "project management", "time management", "adaptability", "creativity", "analytical",
        "collaboration", "mentoring", "agile", "scrum", "presentation", "negotiation"
    ]

    ATS_KEYWORDS = [
        "experience", "skills", "education", "objective", "summary", "professional",
        "responsibilities", "achievements", "certification", "proficient", "managed",
        "developed", "implemented", "designed", "led", "collaborated", "improved",
        "increased", "reduced", "optimized", "bachelor", "master", "degree"
    ]

    ACTION_VERBS = [
        "achieved", "built", "created", "delivered", "engineered", "established",
        "generated", "implemented", "launched", "managed", "optimized", "pioneered",
        "restructured", "spearheaded", "streamlined", "transformed", "developed",
        "designed", "led", "directed", "coordinated", "analyzed", "improved"
    ]

    def __init__(self):
        self.analysis_log = []

    def _log(self, message):
        self.analysis_log.append({
            "timestamp": datetime.now().strftime("%H:%M:%S"),
            "event": message
        })

    def analyze(self, resume_text):
        """
        Main analysis entry point.
        Accepts resume text, returns comprehensive ATS scoring report.
        """
        self.analysis_log = []
        self._log("Starting resume analysis")

        text_lower = resume_text.lower()

        # Extract sections
        sections = self._detect_sections(resume_text)
        self._log("Detected {} resume sections".format(len(sections)))

        # Skill extraction
        found_tech = self._extract_tech_skills(text_lower)
        found_soft = self._extract_soft_skills(text_lower)
        self._log("Found {} tech skills, {} soft skills".format(
            sum(len(v) for v in found_tech.values()), len(found_soft)
        ))

        # ATS keyword analysis
        ats_analysis = self._ats_keyword_analysis(text_lower)
        self._log("ATS keyword match: {}/{} ({:.0f}%)".format(
            ats_analysis["matched"], ats_analysis["total"],
            ats_analysis["match_percentage"]
        ))

        # Action verb analysis
        action_analysis = self._action_verb_analysis(text_lower)

        # Format analysis
        format_analysis = self._analyze_format(resume_text)

        # Compute scores
        ats_score = self._compute_ats_score(
            sections, found_tech, found_soft,
            ats_analysis, action_analysis, format_analysis
        )

        # Skill gap analysis
        skill_gaps = self._skill_gap_analysis(found_tech)

        # Standardized Output Fields
        final_report = {
            "module_name": "Resume ATS Scoring Engine",
            "confidence_score": 1.0,
            "reasoning": [
                "Detected standard resume sections: {}".format(", ".join([k for k, v in sections.items() if v])),
                "Extracted {} technical skills and {} soft skills".format(len(found_tech), len(found_soft)),
                "ATS keyword match density: {}%".format(ats_analysis["match_percentage"])
            ],
            "structured_analysis": {
                "ats_score": ats_score,
                "score_grade": self._score_grade(ats_score),
                "skills": {
                    "technical": found_tech,
                    "soft": found_soft
                },
                "keyword_analysis": ats_analysis,
                "recommendations": self._generate_recommendations(
                    sections, found_tech, found_soft, ats_analysis,
                    action_analysis, format_analysis, ats_score
                )
            },
            "risk_score": float(100 - ats_score),
            "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        }

        return json.dumps(final_report, indent=2)

    def _detect_sections(self, text):
        """Detect standard resume sections."""
        section_patterns = {
            "contact": r"(?i)(email|phone|address|contact|linkedin|github|portfolio)",
            "summary": r"(?i)(summary|objective|profile|about\s+me|professional\s+summary)",
            "experience": r"(?i)(experience|work\s+history|employment|career\s+history)",
            "education": r"(?i)(education|academic|university|college|degree|school)",
            "skills": r"(?i)(skills|technical\s+skills|competencies|proficiencies|expertise)",
            "certifications": r"(?i)(certification|certificate|accreditation|license)",
            "projects": r"(?i)(project|portfolio|work\s+sample|case\s+study)",
            "achievements": r"(?i)(achievement|award|honor|recognition|accomplishment)",
        }

        found = {}
        for section, pattern in section_patterns.items():
            if re.search(pattern, text):
                found[section] = True
            else:
                found[section] = False

        return found

    def _extract_tech_skills(self, text_lower):
        """Extract technical skills by category."""
        found = {}
        for category, skills in self.TECH_SKILLS.items():
            matched = [s for s in skills if s in text_lower]
            if matched:
                found[category] = matched
        return found

    def _extract_soft_skills(self, text_lower):
        """Extract soft skills."""
        return [s for s in self.SOFT_SKILLS if s in text_lower]

    def _ats_keyword_analysis(self, text_lower):
        """Analyze presence of ATS-friendly keywords."""
        matched = [kw for kw in self.ATS_KEYWORDS if kw in text_lower]
        return {
            "matched": len(matched),
            "total": len(self.ATS_KEYWORDS),
            "match_percentage": round(len(matched) / len(self.ATS_KEYWORDS) * 100, 1),
            "found_keywords": matched,
            "missing_keywords": [kw for kw in self.ATS_KEYWORDS if kw not in text_lower][:10]
        }

    def _action_verb_analysis(self, text_lower):
        """Analyze use of strong action verbs."""
        found = [v for v in self.ACTION_VERBS if v in text_lower]
        return {
            "count": len(found),
            "found": found,
            "score": min(100, len(found) * 10),
            "suggested": [v for v in self.ACTION_VERBS if v not in text_lower][:5]
        }

    def _analyze_format(self, text):
        """Analyze resume formatting quality."""
        lines = text.splitlines()
        return {
            "line_count": len(lines),
            "has_bullet_points": bool(re.search(r'[•\-\*]\s', text)),
            "has_numbers": bool(re.search(r'\d+%|\d+\+|#\d+|\$[\d,]+', text)),
            "has_dates": bool(re.search(r'(19|20)\d{2}', text)),
            "avg_line_length": round(sum(len(l) for l in lines) / max(len(lines), 1), 1),
            "excessive_whitespace": text.count('\n\n\n') > 2,
        }

    def _compute_ats_score(self, sections, tech_skills, soft_skills,
                           ats_analysis, action_analysis, format_analysis):
        """Compute overall ATS compatibility score (0-100)."""
        score = 0

        # Section coverage (30 pts)
        essential = ["experience", "education", "skills", "summary"]
        for sec in essential:
            if sections.get(sec, False):
                score += 7.5

        # Technical skills (20 pts)
        tech_count = sum(len(v) for v in tech_skills.values())
        score += min(20, tech_count * 2)

        # Soft skills (10 pts)
        score += min(10, len(soft_skills) * 2.5)

        # ATS keywords (20 pts)
        score += ats_analysis["match_percentage"] * 0.2

        # Action verbs (10 pts)
        score += min(10, action_analysis["count"] * 1.5)

        # Format quality (10 pts)
        if format_analysis["has_bullet_points"]:
            score += 3
        if format_analysis["has_numbers"]:
            score += 3
        if format_analysis["has_dates"]:
            score += 2
        if not format_analysis["excessive_whitespace"]:
            score += 2

        return round(min(100, score), 1)

    def _score_grade(self, score):
        """Convert score to letter grade."""
        if score >= 90:
            return "A+"
        elif score >= 80:
            return "A"
        elif score >= 70:
            return "B+"
        elif score >= 60:
            return "B"
        elif score >= 50:
            return "C"
        elif score >= 40:
            return "D"
        return "F"

    def _skill_gap_analysis(self, found_tech):
        """Identify skill gaps by category."""
        gaps = {}
        for category, all_skills in self.TECH_SKILLS.items():
            found = found_tech.get(category, [])
            missing = [s for s in all_skills if s not in found]
            if missing:
                gaps[category] = {
                    "known": found,
                    "missing": missing[:5],
                    "coverage": "{}%".format(round(len(found) / len(all_skills) * 100))
                }
        return gaps

    def _generate_recommendations(self, sections, tech, soft, ats, action, fmt, score):
        """Generate actionable recommendations."""
        recs = []

        if not sections.get("summary", False):
            recs.append("Add a Professional Summary section — it's the first thing ATS systems scan")

        if not sections.get("skills", False):
            recs.append("Add a dedicated Skills section with clear categorization")

        if ats["match_percentage"] < 50:
            recs.append("Include more industry-standard keywords: {}".format(
                ", ".join(ats["missing_keywords"][:5])
            ))

        if action["count"] < 5:
            recs.append("Use stronger action verbs: {}".format(
                ", ".join(action["suggested"][:5])
            ))

        if not fmt["has_numbers"]:
            recs.append("Quantify achievements with numbers (e.g., 'increased sales by 30%')")

        if not fmt["has_bullet_points"]:
            recs.append("Use bullet points for better readability and ATS parsing")

        if len(soft) < 3:
            recs.append("Highlight more soft skills like leadership, teamwork, communication")

        if not recs:
            recs.append("Excellent resume! Consider tailoring keywords for specific job descriptions")

        return recs


    def tailor_resume(self, resume_text, job_description):
        """
        Tailors a resume based on a job description.
        Extracts required keywords from JD and matches with resume.
        """
        self.analysis_log = []
        self._log("Starting resume tailoring process")
        
        # Extract keywords from job description
        jd_keywords = self._ats_keyword_analysis(job_description.lower())["found_keywords"]
        resume_keywords = self._ats_keyword_analysis(resume_text.lower())["found_keywords"]
        
        missing = [kw for kw in jd_keywords if kw not in resume_keywords]
        
        # Mocking a tailored version (in a real app, this would use templates)
        tailored_summary = "Experienced professional with expertise in {}. Strong background in {}.".format(
            ", ".join(resume_keywords[:3]), ", ".join(jd_keywords[:3])
        )
        
        result = {
            "module_name": "Resume Tailoring Engine",
            "original_match_score": self._compute_ats_score({}, {}, [], {"match_percentage": 0}, {"count": 0}, {"has_bullet_points": True, "has_numbers": True, "has_dates": True, "excessive_whitespace": False}), # Simplified
            "missing_critical_keywords": missing[:10],
            "tailored_summary_suggestion": tailored_summary,
            "improvement_steps": [
                "Integrate missing keywords: {}".format(", ".join(missing[:5])),
                "Update professional summary to highlight relevance to the job description",
                "Ensure recent projects emphasize skills: {}".format(", ".join(jd_keywords[:3]))
            ],
            "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        }
        
        return json.dumps(result, indent=2)


# === ENTRY POINT FOR CHAQUOPY ===
_engine = None

def get_engine():
    global _engine
    if _engine is None:
        _engine = ResumeEngine()
    return _engine

def analyze_resume(resume_text):
    """Entry point: analyze resume and return ATS scoring report."""
    engine = get_engine()
    return engine.analyze(resume_text)

def tailor_resume(resume_text, job_description):
    """Entry point: tailor resume for a specific job description."""
    engine = get_engine()
    return engine.tailor_resume(resume_text, job_description)

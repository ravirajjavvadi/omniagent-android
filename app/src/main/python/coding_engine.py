"""
OmniAgent — Coding Analysis Engine
Performs AST parsing, code complexity analysis, and quality assessment.
Fully offline, returns structured JSON reports.
"""

import ast
import json
import re
from datetime import datetime


class CodingEngine:
    """Analyzes code structure, complexity, and quality using AST parsing."""

    def __init__(self):
        self.analysis_log = []

    def _log(self, message):
        self.analysis_log.append({
            "timestamp": datetime.now().strftime("%H:%M:%S"),
            "event": message
        })

    def analyze(self, code_input):
        """
        Main analysis entry point.
        Accepts code string, returns comprehensive JSON report.
        """
        self._log("Starting code analysis")
        
        intermediate = {
            "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "language_detected": "Unknown"
        }

        try:
            # Try AST parsing (Python code)
            tree = ast.parse(code_input)
            intermediate["language_detected"] = "Python"
            intermediate["analysis"] = self._analyze_ast(tree, code_input)
            self._log("AST analysis completed successfully")
        except SyntaxError as e:
            # Not valid Python — do pattern-based analysis
            intermediate["language_detected"] = "Unknown/Generic"
            intermediate["analysis"] = self._analyze_generic(code_input)
            intermediate["ast_error"] = str(e)
            self._log("AST failed, using pattern-based analysis")

        quality_score = self._compute_quality_score(intermediate["analysis"])
        recommendations = self._generate_recommendations(intermediate["analysis"])

        # Standardized Output Fields
        final_report = {
            "module_name": "Coding Analysis Engine",
            "confidence_score": 1.0,
            "reasoning": [
                "Detected language: {}".format(intermediate["language_detected"]),
                "Parsing strategy: {}".format("AST-Deep" if "ast_error" not in intermediate else "Regex-Pattern"),
                "Metrics summary: {} lines analyzed".format(intermediate["analysis"]["metrics"]["total_lines"])
            ],
            "structured_analysis": {
                "metrics": intermediate["analysis"]["metrics"],
                "quality_score": quality_score,
                "issues": intermediate["analysis"].get("issues", []),
                "recommendations": recommendations
            },
            "risk_score": float(100 - quality_score),
            "timestamp": intermediate["timestamp"]
        }

        return json.dumps(final_report, indent=2)

    def _analyze_ast(self, tree, source_code):
        """Deep analysis using Python AST."""
        analysis = {
            "structure": {
                "classes": [],
                "functions": [],
                "imports": [],
                "global_variables": []
            },
            "metrics": {
                "total_lines": len(source_code.splitlines()),
                "code_lines": 0,
                "comment_lines": 0,
                "blank_lines": 0,
                "function_count": 0,
                "class_count": 0,
                "import_count": 0,
                "max_nesting_depth": 0,
                "avg_function_length": 0
            },
            "issues": []
        }

        lines = source_code.splitlines()
        for line in lines:
            stripped = line.strip()
            if not stripped:
                analysis["metrics"]["blank_lines"] += 1
            elif stripped.startswith("#"):
                analysis["metrics"]["comment_lines"] += 1
            else:
                analysis["metrics"]["code_lines"] += 1

        function_lengths = []

        for node in ast.walk(tree):
            if isinstance(node, ast.ClassDef):
                methods = [n.name for n in node.body if isinstance(n, (ast.FunctionDef, ast.AsyncFunctionDef))]
                analysis["structure"]["classes"].append({
                    "name": node.name,
                    "line": node.lineno,
                    "methods": methods,
                    "method_count": len(methods)
                })
                analysis["metrics"]["class_count"] += 1

            elif isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
                args = [a.arg for a in node.args.args]
                end_line = getattr(node, 'end_lineno', node.lineno + 5)
                func_length = end_line - node.lineno + 1
                function_lengths.append(func_length)

                func_info = {
                    "name": node.name,
                    "line": node.lineno,
                    "args": args,
                    "arg_count": len(args),
                    "length_lines": func_length,
                    "is_async": isinstance(node, ast.AsyncFunctionDef),
                    "has_docstring": False
                }

                # Check for docstring and extract it
                if (node.body and
                    isinstance(node.body[0], ast.Expr) and 
                    isinstance(node.body[0].value, ast.Constant) and 
                    isinstance(node.body[0].value.value, str)):
                    func_info["has_docstring"] = True
                    func_info["docstring_length"] = len(node.body[0].value.value)

                analysis["structure"]["functions"].append(func_info)
                analysis["metrics"]["function_count"] += 1

                # Check for long functions
                if func_length > 50:
                    analysis["issues"].append({
                        "type": "long_function",
                        "severity": "warning",
                        "message": "Function '{}' is {} lines long (>50)".format(node.name, func_length),
                        "line": node.lineno
                    })

                # Check for too many arguments
                if len(args) > 5:
                    analysis["issues"].append({
                        "type": "too_many_args",
                        "severity": "warning",
                        "message": "Function '{}' has {} arguments (>5)".format(node.name, len(args)),
                        "line": node.lineno
                    })

            elif isinstance(node, (ast.Import, ast.ImportFrom)):
                if isinstance(node, ast.Import):
                    for alias in node.names:
                        analysis["structure"]["imports"].append(alias.name)
                else:
                    module = node.module or ""
                    for alias in node.names:
                        analysis["structure"]["imports"].append("{}.{}".format(module, alias.name))
                analysis["metrics"]["import_count"] += 1

            elif isinstance(node, ast.Assign) and hasattr(node, 'col_offset') and node.col_offset == 0:
                for target in node.targets:
                    if isinstance(target, ast.Name):
                        analysis["structure"]["global_variables"].append({
                            "name": target.id,
                            "line": node.lineno
                        })

        # Compute nesting depth
        analysis["metrics"]["max_nesting_depth"] = self._compute_max_depth(tree)

        if function_lengths:
            analysis["metrics"]["avg_function_length"] = round(
                sum(function_lengths) / len(function_lengths), 1
            )

        return analysis

    def _compute_max_depth(self, tree, depth=0):
        """Compute maximum nesting depth of the AST."""
        max_d = depth
        for node in ast.iter_child_nodes(tree):
            if isinstance(node, (ast.For, ast.While, ast.If, ast.With, ast.Try)):
                child_depth = self._compute_max_depth(node, depth + 1)
                max_d = max(max_d, child_depth)
            else:
                child_depth = self._compute_max_depth(node, depth)
                max_d = max(max_d, child_depth)
        return max_d

    def _analyze_generic(self, code_input):
        """Pattern-based analysis for non-Python code."""
        lines = code_input.splitlines()
        analysis = {
            "metrics": {
                "total_lines": len(lines),
                "code_lines": sum(1 for l in lines if l.strip() and not l.strip().startswith(('//', '#', '*', '/*'))),
                "blank_lines": sum(1 for l in lines if not l.strip()),
                "comment_lines": sum(1 for l in lines if l.strip().startswith(('//', '#', '*', '/*'))),
            },
            "patterns_detected": {
                "functions": len(re.findall(r'\b(?:function|def|func|fn)\s+\w+', code_input)),
                "classes": len(re.findall(r'\b(?:class|struct|interface)\s+\w+', code_input)),
                "loops": len(re.findall(r'\b(?:for|while|foreach|loop)\b', code_input)),
                "conditionals": len(re.findall(r'\b(?:if|else|switch|case|match)\b', code_input)),
                "variables": len(re.findall(r'\b(?:var|let|const|val|int|string|float|bool)\s+\w+', code_input)),
            },
            "issues": []
        }

        # Check for common issues
        if analysis["metrics"]["total_lines"] > 300:
            analysis["issues"].append({
                "type": "large_file",
                "severity": "info",
                "message": "File has {} lines — consider splitting into modules".format(
                    analysis["metrics"]["total_lines"]
                )
            })

        return analysis

    def _compute_quality_score(self, analysis):
        """Compute overall quality score out of 100."""
        score = 100
        issues = analysis.get("issues", [])

        for issue in issues:
            if issue["severity"] == "error":
                score -= 15
            elif issue["severity"] == "warning":
                score -= 8
            elif issue["severity"] == "info":
                score -= 3

        metrics = analysis.get("metrics", {})
        code_lines = metrics.get("code_lines", 0)
        comment_lines = metrics.get("comment_lines", 0)

        if code_lines > 0:
            comment_ratio = comment_lines / code_lines
            if comment_ratio < 0.05 and code_lines > 20:
                score -= 5  # Low documentation

        return max(0, min(100, score))

    def _generate_recommendations(self, analysis):
        """Generate actionable recommendations."""
        recs = []
        issues = analysis.get("issues", [])
        metrics = analysis.get("metrics", {})

        if any(i["type"] == "long_function" for i in issues):
            recs.append("Break long functions into smaller, focused functions (aim for <30 lines)")

        if any(i["type"] == "too_many_args" for i in issues):
            recs.append("Consider using a data class or config object to reduce function parameters")

        if metrics.get("comment_lines", 0) == 0 and metrics.get("code_lines", 0) > 10:
            recs.append("Add documentation comments to improve code maintainability")

        if metrics.get("max_nesting_depth", 0) > 4:
            recs.append("Reduce nesting depth — consider early returns or extracting logic")

        if not recs:
            recs.append("Code looks well-structured! Keep following best practices.")

        return recs


# === ENTRY POINT FOR CHAQUOPY ===
_engine = None

def get_engine():
    global _engine
    if _engine is None:
        _engine = CodingEngine()
    return _engine

def analyze_code(code_input, history=None):
    """Entry point: analyze code and return JSON report."""
    engine = get_engine()
    return engine.analyze(code_input)

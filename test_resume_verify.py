import json
import sys
import os

# Add the project's python directory to sys.path
sys.path.append(os.path.abspath(r"c:\Users\RAVIRAJ JAVVADI\Downloads\android-main\android-main\app\src\main\python"))

from resume_engine import ResumeEngine

def test_tailor_resume():
    engine = ResumeEngine()
    resume_text = "I am a Java developer with experience in Spring and SQL."
    job_description = "Seeking a Kotlin developer with experience in Android and MySQL."
    
    result_json = engine.tailor_resume(resume_text, job_description)
    result = json.loads(result_json)
    
    print("Result Keys:", result.keys())
    
    assert "structured_analysis" in result, "Missing structured_analysis key"
    assert "tailored_summary_suggestion" in result["structured_analysis"], "Missing tailored_summary_suggestion"
    assert "improvement_steps" in result["structured_analysis"], "Missing improvement_steps"
    
    print("Test Passed: structured_analysis present and contains expected keys.")
    print("Summary Suggestion:", result["structured_analysis"]["tailored_summary_suggestion"])
    print("Improvement Steps:", result["structured_analysis"]["improvement_steps"])

if __name__ == "__main__":
    test_tailor_resume()

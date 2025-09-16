import React, { useState, useEffect } from "react";

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ||
  (import.meta.env.PROD
    ? "https://terms-conditions-analyser-zrfr.onrender.com"
    : "http://localhost:8080");

function riskToColor(score) {
  if (score >= 0.7) return "rgba(255,0,0,0.15)";
  if (score >= 0.5) return "rgba(255,165,0,0.12)";
  if (score >= 0.25) return "rgba(255,255,0,0.12)";
  return "transparent";
}

function getRiskLabel(score) {
  if (score >= 0.7) return { label: "HIGH RISK", color: "#d9534f" };
  if (score >= 0.5) return { label: "MEDIUM RISK", color: "#f0ad4e" };
  if (score >= 0.25) return { label: "LOW RISK", color: "#5bc0de" };
  return { label: "MINIMAL RISK", color: "#5cb85c" };
}

export default function App() {
  const [file, setFile] = useState(null);
  const [docId, setDocId] = useState(null);
  const [clauses, setClauses] = useState([]);
  const [highRiskClauses, setHighRiskClauses] = useState([]);
  const [pastedText, setPastedText] = useState("");
  const [status, setStatus] = useState("");

  const [loadingUpload, setLoadingUpload] = useState(false);
  const [loadingPaste, setLoadingPaste] = useState(false);
  const [error, setError] = useState("");
  const [stats, setStats] = useState(null);
  const [currentDocumentStats, setCurrentDocumentStats] = useState(null);

  // Filter for high-risk clauses whenever the main 'clauses' state changes
  useEffect(() => {
    const filtered = clauses.filter(c => c.riskScore >= 0.7);
    setHighRiskClauses(filtered);
  }, [clauses]);

  useEffect(() => {
    fetchStats();
  }, []);

  const fetchStats = async () => {
    try {
      // const res = await fetch("/api/documents/stats");
      const res = await fetch(`${API_BASE_URL}/api/documents/stats`);

      if (res.ok) {
        const data = await res.json();
        setStats(data);
      }
    } catch (error) {
      // Failed silently as it's not critical for the main functionality
    }
  };

  const clearState = () => {
    setDocId(null);
    setClauses([]);
    setHighRiskClauses([]);
    setCurrentDocumentStats(null);
    setError("");
    setStatus("");
  };

  const uploadFile = async () => {
    clearState();

    if (!file) {
      setError("Please choose a file");
      return;
    }
    // change
    setLoadingUpload(true);
    setStatus("Uploading and analyzing document...");
    
    try {
      const form = new FormData();
      form.append("file", file);
      
      // const res = await fetch("/api/documents/upload", { 
        const res = await fetch(`${API_BASE_URL}/api/documents/upload`, {
        method: "POST", 
        body: form 
      });
      
      const data = await res.json();
      
      if (!res.ok) {
        throw new Error(data.error || "Upload failed");
      }
      
      setDocId(data.documentId);
      setCurrentDocumentStats({
        totalClauses: data.clausesFound,
        highRiskClauses: data.highRiskClauses
      });
      setStatus(`Processing complete! Found ${data.clausesFound} clauses in ${data.filename}`);
      
      await fetchClauses(data.documentId);
      await fetchStats();
      
    } catch (error) {
      setError(error.message);
      setStatus("");
    } finally {
      // change
      setLoadingUpload(false);
    }
  };

  const pasteText = async () => {
    clearState();
    
    if (!pastedText.trim()) {
      setError("Please paste some text first");
      return;
    }
    
    if (pastedText.length < 50) {
      setError("Text is too short. Please paste a complete terms and conditions document.");
      return;
    }
    // change
    setLoadingPaste(true);
    // setLoading(true);
    setStatus("Analyzing pasted text...");
    
    try {
      const res = await fetch(`${API_BASE_URL}/api/documents/paste`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ content: pastedText })
      });
      
      const data = await res.json();
      
      if (!res.ok) {
        throw new Error(data.error || "Processing failed");
      }
      
      setDocId(data.documentId);
      setCurrentDocumentStats({
        totalClauses: data.clausesFound,
        highRiskClauses: data.highRiskClauses
      });
      setStatus(`Processing complete! Found ${data.clausesFound} clauses.`);
      
      await fetchClauses(data.documentId);
      await fetchStats();
      
    } catch (error) {
      setError(error.message);
      setStatus("");
    } finally {
      // change
      // setLoading(false);
      setLoadingPaste(false);
    }
  };

  const fetchClauses = async (id) => {
    try {
      // const res = await fetch(`/api/documents/${id}/clauses`);
      const res = await fetch(`${API_BASE_URL}/api/documents/${id}/clauses`);
      if (res.ok) {
        const list = await res.json();
        setClauses(list);
      } else {
        const errorData = await res.json();
        throw new Error("Failed to fetch clauses: " + (errorData.error || res.statusText));
      }
    } catch (error) {
      setError("Failed to load clauses: " + error.message);
    }
  };

  return (
    <div style={{
      padding: "32px",
      fontFamily: "Inter, system-ui, Arial",
      minHeight: "100vh",
      width: "100%",
      backgroundColor: "#fdfdfd"
    }}>
      <header style={{ textAlign: "center", marginBottom: 32 }}>
        <h1 style={{ color: "#2c3e50", marginBottom: 8 }}>Digital Consent & Privacy Tracker</h1>
        <p style={{ color: "#7f8c8d", fontSize: 18 }}>
          Analyze Terms & Conditions to identify privacy risks and data usage concerns
        </p>
      </header>

      {/* Global Stats Display */}
      {/* {stats && (
        <div style={{
          background: "#f8f9fa",
          padding: 20,
          borderRadius: 8,
          marginBottom: 24,
          display: "flex",
          justifyContent: "space-around",
          textAlign: "center"
        }}>
          <div>
            <div style={{ fontSize: 24, fontWeight: "bold", color: "#2c3e50" }}>{stats.totalClauses}</div>
            <div style={{ color: "#7f8c8d" }}>Total Clauses Analyzed</div>
          </div>
          <div>
            <div style={{ fontSize: 24, fontWeight: "bold", color: "#e74c3c" }}>{stats.highRiskClauses}</div>
            <div style={{ color: "#7f8c8d" }}>Total High Risk Clauses</div>
          </div>
        </div>
      )} */}

      {/* Current Document Stats Display */}
      {currentDocumentStats && (
        <div style={{
          background: "#e8f5e8",
          padding: 20,
          borderRadius: 8,
          marginBottom: 24,
          border: "2px solid #27ae60"
        }}>
          <h3 style={{ margin: "0 0 12px 0", color: "#27ae60" }}> Document Analysis</h3>
          <div style={{
            display: "flex",
            justifyContent: "space-around",
            textAlign: "center"
          }}>
            <div>
              <div style={{ fontSize: 20, fontWeight: "bold", color: "#2c3e50" }}>
                {currentDocumentStats.totalClauses}
              </div>
              <div style={{ color: "#7f8c8d" }}>Clauses in Document</div>
            </div>
            <div>
              <div style={{ fontSize: 20, fontWeight: "bold", color: "#e74c3c" }}>
                {currentDocumentStats.highRiskClauses}
              </div>
              <div style={{ color: "#7f8c8d" }}>High Risk Clauses</div>
            </div>
          </div>
        </div>
      )}

      {/* File Upload Section */}
      <div style={{ marginBottom: 24, padding: 20, background: "#ffffff", borderRadius: 8, boxShadow: "0 2px 4px rgba(0,0,0,0.1)" }}>
        <h3 style={{ marginTop: 0, color: "#2c3e50" }}>Upload Document</h3>
        <p style={{ color: "#7f8c8d", marginBottom: 16 }}>
          Supports PDF, DOCX, DOC, and TXT files (Must be less than 10MB)
        </p>
        <div style={{ display: "flex", gap: 12, alignItems: "center" }}>
          <input
            type="file"
            onChange={e => setFile(e.target.files[0])}
            accept=".pdf,.docx,.doc,.txt"
            style={{ flex: 1 }}
          />
          <button
            onClick={uploadFile}
            disabled={loadingUpload}
            style={{
              padding: "8px 16px",
              background: loadingUpload ? "#bdc3c7" : "#3498db",
              // change added below
              
              color: "white",
              border: "none",
              borderRadius: 4,
             
              cursor: loadingUpload ? "not-allowed" : "pointer"
            }}
          >
            {loadingUpload ? "Processing..." : "Upload & Analyze"}
            
          </button>
        </div>
      </div>

      {/* Text Paste Section */}
      <div style={{ marginBottom: 24, padding: 20, background: "#ffffff", borderRadius: 8, boxShadow: "0 2px 4px rgba(0,0,0,0.1)" }}>
        <h3 style={{ marginTop: 0, color: "#2c3e50" }}>Paste Text</h3>
        <p style={{ color: "#7f8c8d", marginBottom: 16 }}>
          Copy and paste terms & conditions text directly
        </p>
        <textarea
          placeholder="Paste your terms & conditions text here..."
          value={pastedText}
          onChange={e => setPastedText(e.target.value)}
          rows={6}
          style={{
            width: "100%",
            padding: 12,
            border: "1px solid #ddd",
            borderRadius: 4,
            fontSize: 14,
            fontFamily: "inherit",
            resize: "vertical"
          }}
        />
        <button
          onClick={pasteText}
          disabled={loadingPaste}
          style={{
            marginTop: 12,
            padding: "8px 16px",
            background: loadingPaste ? "#bdc3c7" : "#27ae60",
            
            // change added

            color: "white",
            border: "none",
            borderRadius: 4,
           
            cursor: loadingPaste ? "not-allowed" : "pointer"

          }}
        >
          {loadingPaste ? "Processing..." : "Analyze Text"}
        </button>
      </div>

      {/* Status and Error Display */}
      {error && (
        <div style={{
          padding: 12,
          background: "#fee",
          border: "1px solid #fcc",
          borderRadius: 4,
          color: "#c33",
          marginBottom: 16
        }}>
          ❌ {error}
        </div>
      )}

      {status && !error && (
        <div style={{
          padding: 12,
          background: "#efe",
          border: "1px solid #cfc",
          borderRadius: 4,
          color: "#363",
          marginBottom: 16
        }}>
          ✅ {status}
        </div>
      )}

      {/* NEW: High Risk Clauses Summary */}
      {highRiskClauses.length > 0 && !loadingUpload && !loadingPaste && (
        <div style={{
          marginBottom: 24,
          padding: 20,
          background: "#fff",
          borderRadius: 8,
          boxShadow: "0 2px 4px rgba(0,0,0,0.1)",
          border: "1px solid #d9534f"
        }}>
          <h3 style={{
            color: "#d9534f",
            marginBottom: 16,
            display: "flex",
            alignItems: "center",
            gap: 8,
            margin: 0
          }}>
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#d9534f" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
              <line x1="12" y1="9" x2="12" y2="13"></line>
              <line x1="12" y1="17" x2="12.01" y2="17"></line>
            </svg>
            High Risk Clauses ({highRiskClauses.length})
          </h3>
          <p style={{ color: "#7f8c8d", fontSize: 14 }}>
            These clauses were flagged as high-risk and require careful review.
          </p>
          <div style={{
            display: "grid",
            gridTemplateColumns: "1fr",
            gap: "16px",
            marginTop: 16
          }}>
            {highRiskClauses.map(c => {
              const risk = getRiskLabel(c.riskScore);
              return (
                <div key={c.id} style={{
                  padding: "12px 16px",
                  borderRadius: "6px",
                  background: riskToColor(c.riskScore),
                  borderLeft: `4px solid ${risk.color}`
                }}>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 4 }}>
                    <div style={{ fontWeight: 600, color: "#2c3e50" }}>
                      {c.category}
                    </div>
                    <div style={{ fontSize: 12, color: "#7f8c8d" }}>
                      Risk: {(c.riskScore * 100).toFixed(0)}%
                    </div>
                  </div>
                  <p style={{ margin: "0", color: "#34495e", lineHeight: 1.5, fontSize: 14 }}>
                    {c.clauseText}
                  </p>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Main Clauses Display */}
      {/* change beloww */}
      {docId && !loadingUpload && !loadingPaste && (
        <div>
          {clauses.length > 0 ? (
            <>
              <h2 style={{ color: "#2c3e50", marginBottom: 16 }}>
                Full Analysis Results ({clauses.length} clauses found)
              </h2>
              
              <div style={{ display: "flex", gap: 16, marginBottom: 16, fontSize: 14 }}>
                <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
                  <div style={{ width: 12, height: 12, background: "#d9534f", borderRadius: 2 }}></div>
                  High Risk
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
                  <div style={{ width: 12, height: 12, background: "#f0ad4e", borderRadius: 2 }}></div>
                  Medium Risk
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
                  <div style={{ width: 12, height: 12, background: "#5bc0de", borderRadius: 2 }}></div>
                  Low Risk
                </div>
              </div>
              
              <div style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fit, minmax(350px, 1fr))",
                gap: "20px",
              }}>
                {clauses.map(c => {
                  const risk = getRiskLabel(c.riskScore);
                  return (
                    <div key={c.id} style={{
                      borderLeft: `4px solid ${risk.color}`,
                      padding: "16px",
                      borderRadius: "8px",
                      background: riskToColor(c.riskScore),
                      boxShadow: "0 2px 6px rgba(0,0,0,0.1)"
                    }}>
                      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
                        <div style={{ 
                          fontWeight: 600, 
                          color: "#2c3e50",
                          display: "flex",
                          alignItems: "center",
                          gap: 8
                        }}>
                          {c.category}
                          <span style={{ 
                            fontSize: 11, 
                            padding: "2px 6px", 
                            background: risk.color, 
                            color: "white", 
                            borderRadius: 3,
                            fontWeight: "normal"
                          }}>
                            {risk.label}
                          </span>
                        </div>
                        <div style={{ fontSize: 14, color: "#7f8c8d" }}>
                          Risk: {(c.riskScore * 100).toFixed(0)}%
                        </div>
                      </div>
                      <p style={{ margin: "8px 0", color: "#34495e", lineHeight: 1.5 }}>
                        {c.clauseText}
                      </p>
                      <details style={{ marginTop: 8 }}>
                        <summary style={{ 
                          cursor: "pointer", 
                          color: "#3498db", 
                          fontWeight: 500,
                          padding: "4px 0"
                        }}>
                          💡 View Recommendation
                        </summary>
                        <div style={{ 
                          marginTop: 8, 
                          padding: 12, 
                          background: "rgba(52, 152, 219, 0.1)", 
                          borderRadius: 4,
                          color: "#2c3e50"
                        }}>
                          {c.suggestion}
                        </div>
                      </details>
                    </div>
                  );
                })}
              </div>
            </>
          ) : (
            <div style={{ 
              textAlign: "center", 
              padding: 40, 
              color: "#7f8c8d",
              background: "#f8f9fa",
              borderRadius: 8
            }}>
              <h3>No concerning clauses found!</h3>
              <p>This document appears to have minimal privacy risks based on our analysis.</p>
            </div>
          )}
        </div>
      )}

      <footer style={{ 
        marginTop: 40, 
        padding: 20, 
        textAlign: "center", 
        color: "#95a5a6", 
        borderTop: "1px solid #ecf0f1" 
      }}>
        <p>
          <strong>Digital Consent & Privacy Tracker</strong> helps you understand terms and conditions.
          <br />
          Always consult with legal experts for important decisions.
        </p>
      </footer>
    </div>
  );
}

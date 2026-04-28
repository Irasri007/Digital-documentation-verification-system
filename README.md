# 📄 DocVerify – Digital Document Verification System (DDVS)

A lightweight **Digital Document Verification System (DDVS)** that allows users to **issue, store, and verify documents securely using cryptographic hashing**.

This project provides a simple full-stack implementation with:
- 🌐 Frontend (HTML/CSS/JS)
- ⚙️ Backend (Java HTTP Server)
- 🔐 Secure hash-based verification

---

## 🚀 Features

- ✅ Upload and verify documents
- 🔐 SHA-based hashing for integrity checking
- 📁 JSON-based document registry
- 🧾 Issue new documents with metadata
- 🔍 Verify authenticity of uploaded files
- 📊 Audit and health check endpoints
- ⚡ Lightweight Java backend (no heavy frameworks)

---

## 🏗️ Project Structure
DocVerify_DDVS/
│
├── generate_hash_entry.py
│
├── ddvs/
│ ├── frontend/
│ │ └── index.html
│
│ ├── backend/
│ │ ├── src/main/java/com/ddvs/
│ │ │ ├── Main.java
│ │ │ ├── VerifyHandler.java
│ │ │ ├── IssueHandler.java
│ │ │ ├── AuditHandler.java
│ │ │ ├── CryptoUtil.java
│ │ │ ├── Registry.java
│ │ │ └── ...
│ │ └── ddvs-data.json
│
│ ├── ddvs.properties
│ ├── run.bat
│ ├── run.sh
│ └── README.md


---

## ⚙️ How It Works

1. 📤 A document is uploaded  
2. 🔑 A cryptographic hash is generated  
3. 🗄️ Hash + metadata is stored in JSON database  
4. 🔍 During verification:
   - File hash is recomputed  
   - Compared with stored hash  
5. ✅ If matched → Document is authentic  
   ❌ Else → Document is tampered  

---

## 🧑‍💻 Setup Instructions

### 🔧 Prerequisites

- Java (JDK 8 or above)
- Python (optional)

---

### ▶️ Run Backend

#### Windows:

cd ddvs
run.bat


#### Linux / Mac:

cd ddvs
chmod +x run.sh
./run.sh


Server runs at:

http://localhost:8080


---

### 🌐 Run Frontend

Open:

frontend/index.html


Or use Live Server in VS Code.

---

## 📡 API Endpoints

| Endpoint   | Method | Description                  |
|------------|--------|------------------------------|
| /verify    | POST   | Verify uploaded document     |
| /issue     | POST   | Issue new document           |
| /audit     | GET    | View document logs           |
| /health    | GET    | Server health check          |
| /config    | GET    | Config status                |

---

## 🔐 Security

- Uses SHA-based hashing
- Ensures document integrity
- Detects tampering instantly

---

## 🛠️ Utility Script

### Generate Hash Entry


python generate_hash_entry.py


---

## 📊 Future Improvements

- Blockchain integration
- User authentication system
- Cloud deployment
- Mobile-friendly UI
- AI-based document detection

---

## 🎯 Use Cases

- Academic certificate verification  
- Government document validation  
- Digital contracts  
- Identity verification systems  

---

## 👩‍💻 Author

**Iraa Srivastava**  
B.Tech CSE | 2nd Year  

---

## ⭐ Contribution

Feel free to fork and improve this project.

---

## 📜 License

This project is for educational purposes.

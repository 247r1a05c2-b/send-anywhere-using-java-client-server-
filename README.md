# send-anywhere-using-java-client-server-
# 📁 Java File Transfer Application (GUI Based)

A **Java Swing GUI-based Client–Server File Transfer Application** that allows users to transfer files securely and efficiently over a network using **socket programming**.

This project demonstrates core concepts of **Java Networking**, **GUI development**, and **client–server architecture**, making it suitable for academic submissions and internship portfolios.

---

## 🚀 Features

- Client–Server based file transfer system
- Graphical User Interface using Java Swing
- Automatic file sending from client to server
- Supports transfer of large files
- Simple and user-friendly interface
- Real-time status messages
- Error handling for connection issues

---

## 🛠 Technologies Used

- **Java**
- **Java Swing** (GUI)
- **Socket Programming**
- **TCP/IP Networking**
- **Multithreading**

---

## 📂 Project Structure

File-Transfer-Java-GUI/
│
├── FileTransferServerGUI.java # Server-side GUI application
├── FileTransferClientAutoGUI.java # Client-side GUI application
└── README.md

yaml
Copy code

---

## ▶️ How to Run the Project

### 🔹 Prerequisites
- Java JDK 8 or above
- Any Java-supported OS (Windows / Linux / macOS)

---

### 🔹 Step 1: Compile the Files

```bash
javac FileTransferServerGUI.java
javac FileTransferClientAutoGUI.java
🔹 Step 2: Run the Server
bash
Copy code
java FileTransferServerGUI
Start the server before running the client.

🔹 Step 3: Run the Client
bash
Copy code
java FileTransferClientAutoGUI
🔄 Working Explanation
The Server listens for incoming client connections on a specific port.

The Client connects to the server using socket programming.

The client selects a file and sends it automatically.

The server receives and stores the file at the specified location.

GUI updates show the transfer status in real time.

🎯 Use Cases
Learning Java networking concepts

Understanding client–server architecture

Academic mini-project or lab project

Internship and resume project

Secure local file sharing

👨‍💻 Author
Shaik Irfan Hussain
B.Tech CSE Student
GitHub: https://github.com/247r1a05c2-b

# 📡 Instant Messaging System - DAW Project (2024/2025 Fall Semester)

This is a full-stack **multi-user Instant Messaging (IM) system**, developed as part of the **DAW (Desenvolvimento de Aplicações Web)** course at [ISEL](https://www.isel.pt/).

The system is composed of two main components:
- 🖥️ **Backend**, implemented in a JVM-based language with PostgreSQL
- 🌐 **Frontend**, a browser-based web application

---

## 🧠 Project Overview

This IM system allows authenticated users to create and join communication channels, exchange messages, and manage access control. The system supports both **public** and **private** channels with different permission levels.

---

## ⚙️ Architecture

### Backend
- **Technology**: JVM-based application (Kotlin with Spring Boot)
- **Database**: Made with PostgreSQL
- **Responsibilities**:
  - Expose a RESTful HTTP API
  - Handle data access and business logic
  - Enforce domain rules and access control
  - Manage session tokens for authentication
  - Support user registration via one-time invitations

### Frontend
- **Technology**: Web application (React with Typescript)
- **Responsibilities**:
  - User interface for all core functionality
  - Communicates with backend via HTTP API
  - Allows interaction with messages and channels

---

## 🔐 Features

### ✅ User Authentication
- Username + password based login
- Session management using tokens (via HTTP headers or cookies)
- User registration via **invitation**

### 💬 Channels and Messages
- Public and private channels
- Each channel has:
  - A unique identifier
  - A unique, editable display name
  - Access control rules (Public or Private)
  - Owner
- Messages include:
  - Author
  - Associated channel
  - Creation timestamp
  - Content of the message
- Users can:
  - Create and join channels
  - Post and view messages
  - Invite others to private channels with read-only or read-write permissions

### 🖼️ User Interface
Supports:
- Registration and login
- Viewing and managing joined channels
- Viewing and posting messages
- Creating/searching/joining/leaving channels
- Managing invitations

---

## 📨 .mailmap
47076 <a47076@alunos.isel.pt> - Mariana Muñoz
49644 <a49644@alunos.isel.pt> - Rodrigo Lopes
50457 <a50457@alunos.isel.pt> <joaopclopes.17@gmail.com> - João Lopes


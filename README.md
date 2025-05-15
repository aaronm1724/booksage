# BookSage 📚

BookSage is a Java-based command-line application that helps you discover new books based on your interests. Whether you're into epic fantasy, sci-fi thrillers, or contemporary fiction, BookSage connects with the Google Books API to give you curated recommendations based on genre, author, or specific titles.

---

## 🔧 Features

- 🔍 Search by genre to discover popular and recent books
- 👤 Search by author to explore their catalog and similar authors
- 📖 Look up a specific book or series to find related recommendations
- 🧠 Intelligent CLI interface that evolves over time
- 🔌 Built-in support for API integration (Google Books, Goodreads in future)

---

## 📚 Technologies Used

- Java 17+
- Google Books API (REST)
- JSON parsing with Gson or Jackson (TBD)
- CLI interface using Scanner
- Modular architecture with services and models

---

## 🛠 Planned Roadmap

- [x] Basic CLI interface and menu
- [ ] Genre search with Google Books API
- [ ] Author lookup and recommendations
- [ ] Book lookup + similar titles
- [ ] Caching of results locally
- [ ] Optional: migrate to Spring Boot REST API
- [ ] Optional: React + TypeScript frontend

---

## 🚀 Getting Started

```bash
git clone https://github.com/YOUR_USERNAME/BookSage.git
cd BookSage
javac src/Main.java
java src/Main
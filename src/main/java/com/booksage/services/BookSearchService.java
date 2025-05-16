package com.booksage.services;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BookSearchService {
    // HTTP client for API requests
    private final OkHttpClient client;
    // Gson instance for JSON parsing
    private final Gson gson;
    // Base URL for Google Books API
    private static final String GOOGLE_BOOKS_API_URL = "https://www.googleapis.com/books/v1/volumes";
    // Base URL for Goodreads search
    private static final String GOODREADS_SEARCH_URL = "https://www.goodreads.com/search?q=";
    private final Scanner scanner = new Scanner(System.in);

    public BookSearchService() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    /**
     * Searches for books by genre using the Google Books API
     * @param genre The genre to search for
     * @throws IOException if there's an error making the HTTP request
     */
    public void searchByGenre(String genre) throws IOException {
        searchBooks(genre, "genre");
    }

    /**
     * Searches for books by author using the Google Books API
     * @param author The author to search for
     * @throws IOException if there's an error making the HTTP request
     */
    public void searchByAuthor(String author) throws IOException {
        searchBooks(author, "author");
    }

    /**
     * Searches for books by title using the Google Books API
     * @param title The title to search for
     * @throws IOException if there's an error making the HTTP request
     */
    public void searchByTitle(String title) throws IOException {
        searchBooks(title, "title");
    }

    /**
     * Common method to search books using the Google Books API with client-side filtering
     */
    private void searchBooks(String searchTerm, String searchType) throws IOException {
        JsonArray items = fetchBooksFromApi(searchTerm, searchType);
        
        if (items == null || items.size() == 0) {
            System.out.println("\nNo results found for: " + searchTerm);
            return;
        }

        // Store filtered results
        List<JsonObject> filteredBooks = new ArrayList<>();
        Set<String> allValues = new HashSet<>(); // For "did you mean" functionality
        
        // Filter results and collect all possible matches
        for (int i = 0; i < items.size() && filteredBooks.size() < 5; i++) {
            JsonObject bookInfo = items.get(i).getAsJsonObject()
                    .get("volumeInfo").getAsJsonObject();
            
            // Collect all values for the search type for potential suggestions
            collectSearchTypeValues(bookInfo, searchType, allValues);
            
            if (matchesSearchCriteria(bookInfo, searchTerm, searchType, filteredBooks.size())) {
                filteredBooks.add(bookInfo);
            }
        }

        if (filteredBooks.isEmpty()) {
            handleNoResults(searchTerm, searchType, allValues);
            return;
        }

        displayResults(filteredBooks, searchType, searchTerm);
    }

    /**
     * Fetches books from the Google Books API using type-specific query parameters
     * @param searchTerm The term to search for
     * @param searchType The type of search (genre, author, title)
     * @return JsonArray of book items or null if none found
     * @throws IOException if there's an error making the HTTP request
     */
    private JsonArray fetchBooksFromApi(String searchTerm, String searchType) throws IOException {
        // Construct the query based on search type
        String queryPrefix = switch (searchType) {
            case "genre" -> "subject:";
            case "author" -> "inauthor:";
            case "title" -> "intitle:";
            default -> "";
        };
        
        // Build the full query and encode it
        String query = queryPrefix + searchTerm;
        String encodedQuery;
        try {
            encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // Fallback to basic encoding if UTF-8 is somehow not supported
            encodedQuery = searchTerm.replace(" ", "+");
        }

        // Construct the full API URL
        String apiUrl = GOOGLE_BOOKS_API_URL + "?q=" + encodedQuery + "&maxResults=20";

        // Make the API request
        Request request = new Request.Builder()
                .url(apiUrl)
                .build();

        // Execute the request and parse the response
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response);
            }

            String jsonData = response.body().string();
            JsonObject jsonObject = gson.fromJson(jsonData, JsonObject.class);
            return jsonObject.has("items") ? jsonObject.getAsJsonArray("items") : null;
        }
    }

    /**
     * Collects all values of the specified search type from a book
     */
    private void collectSearchTypeValues(JsonObject bookInfo, String searchType, Set<String> values) {
        switch (searchType) {
            case "author":
                if (bookInfo.has("authors")) {
                    JsonArray authors = bookInfo.getAsJsonArray("authors");
                    for (int i = 0; i < authors.size(); i++) {
                        values.add(authors.get(i).getAsString());
                    }
                }
                break;
            case "title":
                if (bookInfo.has("title")) {
                    values.add(bookInfo.get("title").getAsString());
                }
                break;
            case "genre":
                if (bookInfo.has("categories")) {
                    JsonArray categories = bookInfo.getAsJsonArray("categories");
                    for (int i = 0; i < categories.size(); i++) {
                        values.add(categories.get(i).getAsString());
                    }
                }
                break;
        }
    }

    /**
     * Handles the case when no results are found, including "did you mean" functionality
     */
    private void handleNoResults(String searchTerm, String searchType, Set<String> allValues) throws IOException {
        if (allValues.isEmpty()) {
            System.out.println("\nSorry, we couldn't find any matches for: " + searchTerm);
            return;
        }

        String closestMatch = findClosestMatch(searchTerm, allValues);
        if (closestMatch != null) {
            System.out.println("\nNo results found for: " + searchTerm);
            System.out.print("Did you mean: " + closestMatch + "? (y/n): ");
            
            String response = scanner.nextLine().trim().toLowerCase();
            if (response.equals("y")) {
                // Recursive call with the corrected term
                searchBooks(closestMatch, searchType);
                return;
            }
        }
        
        System.out.println("\nSorry, we couldn't find any matches for: " + searchTerm);
    }

    /**
     * Finds the closest matching string using Levenshtein distance
     */
    private String findClosestMatch(String searchTerm, Set<String> candidates) {
        String searchTermLower = searchTerm.toLowerCase();
        int minDistance = Integer.MAX_VALUE;
        String closestMatch = null;

        for (String candidate : candidates) {
            int distance = levenshteinDistance(searchTermLower, candidate.toLowerCase());
            if (distance < minDistance && distance <= searchTerm.length() / 2) { // Max 50% difference
                minDistance = distance;
                closestMatch = candidate;
            }
        }

        return closestMatch;
    }

    /**
     * Calculates Levenshtein distance between two strings
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1)
                    );
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * Displays the search results
     */
    private void displayResults(List<JsonObject> books, String searchType, String searchTerm) {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("Top " + books.size() + " Books matching " + searchType + ": " + searchTerm);
        System.out.println("-".repeat(60));

        for (int i = 0; i < books.size(); i++) {
            JsonObject book = books.get(i);
            
            String title = book.has("title") ? book.get("title").getAsString() : "Unknown Title";
            String authorList = formatAuthors(book);
            String goodreadsLink;
            try {
                goodreadsLink = GOODREADS_SEARCH_URL + 
                    URLEncoder.encode(title + " " + authorList, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                goodreadsLink = GOODREADS_SEARCH_URL + title + " " + authorList;
            }

            System.out.println("\nBook " + (i + 1) + ":");
            System.out.println("Title: " + title);
            System.out.println("Author(s): " + authorList);
            System.out.println("Goodreads: " + goodreadsLink);
        }
        
        System.out.println("\n" + "-".repeat(60));
    }

    /**
     * Formats the author list for display
     */
    private String formatAuthors(JsonObject book) {
        if (!book.has("authors")) return "Unknown Author";
        
        JsonArray authors = book.getAsJsonArray("authors");
        return authors.size() == 1 ? 
            authors.get(0).getAsString() :
            authors.asList().stream()
                .map(author -> author.getAsString())
                .collect(Collectors.joining(", "));
    }

    /**
     * Check if a book matches the author search term
     */
    private boolean matchesAuthor(JsonObject bookInfo, String searchTerm) {
        if (!bookInfo.has("authors")) return false;
        
        JsonArray authors = bookInfo.getAsJsonArray("authors");
        String searchTermLower = searchTerm.toLowerCase();
        
        for (int i = 0; i < authors.size(); i++) {
            if (authors.get(i).getAsString().toLowerCase().contains(searchTermLower)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a book matches the title search term
     */
    private boolean matchesTitle(JsonObject bookInfo, String searchTerm) {
        if (!bookInfo.has("title")) return false;
        
        String title = bookInfo.get("title").getAsString().toLowerCase();
        return title.contains(searchTerm.toLowerCase());
    }

    /**
     * Check if a book matches the genre search term
     * @param bookInfo The book's JSON information
     * @param searchTerm The genre to search for
     * @param currentMatchCount Number of books already matched
     * @return true if the book matches the genre criteria
     */
    private boolean matchesGenre(JsonObject bookInfo, String searchTerm, int currentMatchCount) {
        // If no categories are present, only include if we need more results
        if (!bookInfo.has("categories")) {
            return currentMatchCount < 3; // Be more selective with uncategorized books
        }
        
        // For books with categories, check for partial matches
        JsonArray categories = bookInfo.getAsJsonArray("categories");
        String searchTermLower = searchTerm.toLowerCase();
        
        // Check each category for a partial match
        for (int i = 0; i < categories.size(); i++) {
            String category = categories.get(i).getAsString().toLowerCase();
            // Look for the search term as a substring of the category
            if (category.contains(searchTermLower)) {
                return true;
            }
        }
        
        // If we have few matches so far, be more lenient with related categories
        if (currentMatchCount < 2) {
            for (int i = 0; i < categories.size(); i++) {
                String category = categories.get(i).getAsString().toLowerCase();
                // Check for related terms (e.g., "fiction" for "science fiction")
                if (searchTermLower.contains(category) || 
                    areRelatedGenres(category, searchTermLower)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Check if two genres are related
     * This is a simple implementation that could be expanded with a more comprehensive genre mapping
     */
    private boolean areRelatedGenres(String category, String searchTerm) {
        // Example related genre pairs
        if (category.contains("fiction") && searchTerm.contains("novel")) return true;
        if (category.contains("mystery") && searchTerm.contains("thriller")) return true;
        if (category.contains("sci-fi") && searchTerm.contains("science fiction")) return true;
        if (category.contains("fantasy") && searchTerm.contains("magic")) return true;
        return false;
    }

    /**
     * Check if a book matches the search criteria
     */
    private boolean matchesSearchCriteria(JsonObject bookInfo, String searchTerm, String searchType, int currentMatchCount) {
        switch (searchType) {
            case "author":
                return matchesAuthor(bookInfo, searchTerm);
            case "title":
                return matchesTitle(bookInfo, searchTerm);
            case "genre":
                return matchesGenre(bookInfo, searchTerm, currentMatchCount);
            default:
                return false;
        }
    }
} 
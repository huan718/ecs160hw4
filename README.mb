# HW2 README


---
### 0. Assumptions

> - Redis database is pre-populated (from HW1)
> - We assume Redis contains:

Repository metadata (Url, CreatedAt, AuthorName)

Issue descriptions stored under keys like iss-curl-1, iss-curl-2, etc.
> 
---
---

### 1. GitHub API Token

This program requires a GitHub API token to access repository data. Set it as an environment variable in Linux:

```bash
# Replace YOUR_TOKEN_HERE with actual GitHub personal access/oauth token 
export GITHUB_TOKEN=YOUR_TOKEN_HERE
```

---

### 2. Redis Setup


We assumeed redis is already setup on testing environment

---

### 3. Running the Program


```bash
# Run the main Java program
mvn clean compile exec:java
```

> Full execution may take **~22 minutes** due to API fetch limits and analysis with cloning. 
> To reduce runtime for testing or demonstration, you can limit specs in App.java:  
> - Number of repositories fetched per language  
> - Number of commits analyzed per repository
> - Native Git package was used instead of JGit to speed up cloning

---

### 4. Viewing Results

After running, the program prints statistics per language:

```
Fetching Top 10 Repositorys for Rust
Yes rust-lang/rust (>60% code)
Cloning most popular code repo for ust: rust-lang/rust

Language: Rust
Total stars: ****
Total forks: ****
Total open-issues: ****
Top-3 Most modified files:
Repo name: example-repo
  1. src/Main.java
  2. src/Utils.java
  3. README.md
New commits in forked repos: ****
Open issues in top-10 repos: ****
```

- We used the full file path for displaying the files.
- Repository details are also stored in Redis.

---

### 5. Unit Tests

Unit tests validate statistics computation without invoking the GitHub API. Run tests with:

```bash
mvn test
```

---

### 6. GitHub Actions

- Every push triggers GitHub Actions to run unit tests automatically.  
- Ensures correctness of statistics computation while keeping API calls separate.

---

### 7. Notes on Performance

- The program fetches full repository and fork data, including commits, which is time-consuming.  
- For faster runs during development, reduce:
  - Number of repositories per language (e.g., top 3 instead of top 10)  
  - Number of commits analyzed (e.g., last 10 instead of last 50)  
- Using `--depth 1` for `git clone` significantly reduces cloning time.

---


package com.ecs160.hw.model;

public class Issue {
   private final String bug_type;
   private final int line;
   private final String description;
   private final String filename;

   // Constructor
   public Issue(String bug_type, int line, String description, String filename) {
      this.bug_type = bug_type;
      this.line = line;
      this.description = description;
      this.filename = filename;
   }

   // Public getters
   public String getBugType() { return bug_type; }
   public int getLine() { return line; }
   public String getDescription() { return description; }
   public String getFilename() { return filename; }
}

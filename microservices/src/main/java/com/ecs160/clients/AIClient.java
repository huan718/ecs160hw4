package com.ecs160.clients;

import java.io.IOException;

// Generic client interface
public interface AIClient {
   String ask(String prompt) throws IOException;
}

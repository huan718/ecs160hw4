package com.ecs160.clients;

import java.io.IOException;

public interface AIClient {
   String ask(String prompt) throws IOException;
}

import com.example.microframework.annotations.Microservice;
import com.example.microframework.annotations.Endpoint;

@Microservice
public class ExampleService {

    @Endpoint(url = "/hello")
    public String handleRequest(String input) {
        if (input == null || input.isBlank()) {
            return "Hello! No input provided.";
        }
        return "Hello! You said: " + input;
    }

    @Endpoint(url = "/echo")
    public String echo(String input) {
        return input == null ? "" : input;
    }
}
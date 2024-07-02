import java.io.IOException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class HelloServlet extends HttpServlet {

  @Override
  public void service(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    resp.setContentType("text/plain");

    // no cache
    resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
    resp.setHeader("Pragma", "no-cache"); // HTTP 1.0.
    resp.setHeader("Expires", "0"); // Proxies.

    resp.getWriter().print("TEST_VAR=" + System.getenv("TEST_VAR"));
    resp.getWriter().print("Hello from the App Engine Standard project.");
  }

}

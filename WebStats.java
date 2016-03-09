import java.net.URL;
import java.net.MalformedURLException;

import java.util.Stack;
import java.util.concurrent.*;

public class WebStats {
  public static void main(String[] args) {
    URL url = null;
    try {
      url = new URL(args[4]);
    } catch (MalformedURLException e) {
      System.out.println("ERROR: Bad URL provided");
      e.printStackTrace();
    }

    if (url != null) {
    	WebParser p = new WebParser(url, Integer.parseInt(args[3]), Integer.parseInt(args[1]));
    }
  }
}
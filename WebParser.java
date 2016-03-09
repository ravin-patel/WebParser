import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.concurrent.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class WebParser {
  // Holds html tag and value
  private ConcurrentHashMap<URL, HashMap> urlsWithStats = new ConcurrentHashMap<URL, HashMap>();
  // Counts all htmltags
  private ConcurrentHashMap<String, Integer> TotalStats = new ConcurrentHashMap<String, Integer>();
  //Amount of pages parsed
  private static int pagesParsed = 0;
  //Amount of pages to parse
  private static int pagesToParse = 0;
  //Amount of paths to reach
  private static int pathsToReach = 0;

  /**
   * Constructor
   * @param url
   * @param pagesToParse
   * @param pathstoReach
   */
  public WebParser(URL url, int pagesToParse, int pathsToReach) 
  {
    this.pagesToParse = pagesToParse;
    this.pathsToReach = pathsToReach;
    queuePagesParsedTask(url, 1);
  }

  /**
   * Push URL with Hashpmap to link them to URls
   * @param url, Url to push
   * @param stats, Hashmap to push
   */
  public void pushToUrlsStats(URL url, HashMap stats) 
  {
    urlsWithStats.put(url, stats);
  }

  /**
   * Print all the Urls with the tags
   */
  public void printStats() 
 {
    for (URL key : urlsWithStats.keySet()) 
    {
      System.out.println("URL: " + key.toString());
      for (Object htmlTag : urlsWithStats.get(key).keySet()) 
      {
        System.out.println(htmlTag.toString() + " - " + urlsWithStats.get(key).get(htmlTag).toString());
        incrementTotalStats(htmlTag.toString(), (Integer)(urlsWithStats.get(key).get(htmlTag)));
      }
    }
  }

  /**
   * Increment TotalStats if tag found
   * @param htmlTag
   * @param count
   */
  public void incrementTotalStats(String htmlTag, int count) 
  {
    if (TotalStats.containsKey(htmlTag)) 
    {
      TotalStats.put(htmlTag, TotalStats.get(htmlTag) + count);
    } 
    else 
    {
      TotalStats.put(htmlTag, count);
    }
  }

  /**
   * Organize and order htmlTags
   */
  public void printTotalStats() 
  {
	  for (Entry<String, Integer> entry : TotalStats.entrySet()) 
	  {
          String key = entry.getKey().toString();
          Integer value = entry.getValue();
          System.out.println(key + " ---- " + value );
        }
  }

  /**
   * Queue a new web parse task
   * @param url, the url to parse
   * @param path, the path number deep this url is
   */
  public void queuePagesParsedTask(URL url, int path)
  {
    (new Thread(new PagesParsedJob(url, path))).start();
  }

  /**
   * Retrieve the number of pages parsed
   */
  public static synchronized int pagesParsed() 
  {
    return pagesParsed;
  }

  /**
   * Increment the number of pages parsed
   */
  public static synchronized void incrementpagesParsed()
  {
    pagesParsed++;
  }

  /**
   * Decrement the number of pages parsed
   */
  public static synchronized void decrementpagesParsed() 
  {
    pagesParsed--;
  }

  /**
   * Retrieve the number of pages to parse
   */
  public static synchronized int pagesToParse() 
  {
    return pagesToParse;
  }

  /**
   * Retrieve the number of paths to reach
   */
  public static synchronized int pathsToReach() 
  {
    return pathsToReach;
  }

  public abstract class HtmlParser 
  {
    //Html tag count tracker
    private HashMap<String, Integer> urlStats = new HashMap<String, Integer>();

    /**
     * Parse HTML content
     * @param url, an absolute URL to parse
     */
    public void retrieveAndParseHtml(URL url) 
    {
      HttpURLConnection connection = null;
      try 
      {
        connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept","*/*");
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine, htmlTag;

        Pattern regexTag = Pattern.compile("< ?([A-Za-z]+)");
        Pattern regexHref = Pattern.compile("href=\"((http|https):[^ ]+)\"");
        Matcher tagMatcher;

        while((inputLine = reader.readLine()) != null) 
        {
          tagMatcher = regexTag.matcher(inputLine);

          if (tagMatcher.find()) 
          {
            htmlTag = tagMatcher.group(1);
            incrementCountForHtmlTag(htmlTag);
            if (htmlTag.equals("a")) 
            {
              tagMatcher = regexHref.matcher(inputLine);
              if (tagMatcher.find()) 
              {
                handleFoundLink(tagMatcher.group(1));
              }
            }
          }
        }
        reader.close();
        pushToUrlsStats(url, urlStats);
        printStats();
        printTotalStats();
      } 
      catch (Exception e) 
      {
        System.out.println("ERROR incorrect URL: " + url.toString());
        decrementpagesParsed();
      } 
      finally 
      {
        if (connection != null) 
        {
          connection.disconnect();
          
        }
      }
    }
    /**
     * Increase the stat if tag found; 
     * if not, add tag to urlStats
     * @param htmlTag
     */
    public void incrementCountForHtmlTag(String htmlTag)
    {
      if (urlStats.containsKey(htmlTag)) 
      {
        urlStats.put(htmlTag, urlStats.get(htmlTag) + 1);
      } else 
      {
        urlStats.put(htmlTag, 1);
      }
    }

    
     //Checks if URL is valid.
     // @param url
     
    public Boolean isValidUrl(String url) 
    {
      Boolean isValidUrl = true;
      URL urlChecker = null;

      try
      {
        urlChecker = new URL(url);
      } 
      catch (MalformedURLException e) 
      {
        isValidUrl = false;
      }

      return isValidUrl;
    }

    public abstract void handleFoundLink(String url);
  }

  /**
   * PagesParsedJob implements Runnable
   */
  public class PagesParsedJob extends HtmlParser implements Runnable 
  {
    private final URL url;
    private final int pathNumber;

    /**
     * PagesParsedJob Constructor
     */
    public PagesParsedJob(URL url, int pathNumber)
    {
      this.url = url;
      this.pathNumber = pathNumber;
    }

    /**
     * Run html Parser
     */
    public void run() 
    {
      retrieveAndParseHtml(url);
      incrementpagesParsed();
    }

    /**
     * Checks if URL is valid
     * @param url
     */
    public void handleFoundLink(String url) 
    {
      if (isValidUrl(url) && countinueParsing()) 
      {
        try
        {
          System.out.println("URL: " + url);
          queuePagesParsedTask(new URL(url), pathNumber + 1);
        } 
        catch (MalformedURLException e)
        {
          System.out.println("ERROR incorrect URL: " + url);
        }
      }
    }

    /**
     * Check to see if prgm should continue to parse
     */
    public Boolean countinueParsing() 
    {
      return (pagesParsed() < pagesToParse()) && (pathNumber < pathsToReach());
    }
  }
}
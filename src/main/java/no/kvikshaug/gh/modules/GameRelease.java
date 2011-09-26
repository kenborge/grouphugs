package no.kvikshaug.gh.modules;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import no.kvikshaug.gh.Grouphug;
import no.kvikshaug.gh.ModuleHandler;
import no.kvikshaug.gh.listeners.TriggerListener;
import no.kvikshaug.gh.util.Web;

public class GameRelease implements TriggerListener {

	private static final String TRIGGER = "release";
	private static final String TRIGGER_HELP = "release";
	private Grouphug bot;
	private String apiKey = ""; //TODO: Should be read from config

	public GameRelease(ModuleHandler moduleHandler) {
		bot = Grouphug.getInstance();

		moduleHandler.addTriggerListener(TRIGGER, this);
		moduleHandler.registerHelp(TRIGGER_HELP, "release: looks up the release date for a given game.\n" +
				Grouphug.MAIN_TRIGGER+TRIGGER+" <game>");
	}

	public void onTrigger(String channel, String sender, String login, String hostname, String message, String trigger) {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(DateTime.class, new DateTimeTypeConverter());
		Gson gson = gsonBuilder.create();
		ReleaseSearch search = null;
		try {
			URL url = new URL("http://api.giantbomb.com/search/?api_key=" + apiKey  + "&query='" + URLEncoder.encode(message, "utf-8") + "'&limit=1&field_list=original_release_date,expected_release_year,expected_release_month,name&resources=game&format=json");
			search = gson.fromJson(Web.prepareEncodedBufferedReader(url), ReleaseSearch.class);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(search == null) {
			bot.msg(channel, "Det oppsto en feil");
		}
		if(search.getNumberOfTotalResults() > 0) {
			ReleaseResult result = search.getResults().get(0);
			String answer = result.getName();
			if(result.getOriginalReleaseDate() != null) {
				if(result.getOriginalReleaseDate().isAfterNow())
					answer += " kommer ut ";
				else answer += " kom ut ";
				answer += result.getOriginalReleaseDate().toString("dd.MM.yyyy");
			}
			else if(result.getExpectedReleaseMonth() != null) {
				answer += " kommer ut i måned " + result.getExpectedReleaseMonth();
				if(result.getExpectedReleaseYear() != null) {
					answer += " i " + result.getExpectedReleaseYear();
				}
			}
			else if(result.getExpectedReleaseYear() != null) {
				answer += " kommer i " + result.getExpectedReleaseYear();
			}
			else {
				answer = "Det oppsto en feil";
			}
			bot.msg(channel, answer);
		} else {
			bot.msg(channel, "Fant ikke spillet");
		}
	}

	public static class ReleaseSearch {
		private int number_of_page_results;
		private int status_code;
		private String error;
		private List<ReleaseResult> results;
		private int limit;
		private int offset;
		private int number_of_total_results;

		public int getNumberOfPageResults() {
			return number_of_page_results;
		}
		public int getStatusCode() {
			return status_code;
		}
		public String getError() {
			return error;
		}
		public List<ReleaseResult> getResults() {
			return results;
		}
		public int getLimit() {
			return limit;
		}
		public int getOffset() {
			return offset;
		}
		public int getNumberOfTotalResults() {
			return number_of_total_results;
		}
	}

	public static class ReleaseResult {
		private Integer expected_release_month;
		private Integer expected_release_year;
		private DateTime original_release_date;
		private String name;
		private String resource_type;

		public Integer getExpectedReleaseMonth() {
			return expected_release_month;
		}
		public Integer getExpectedReleaseYear() {
			return expected_release_year;
		}
		public DateTime getOriginalReleaseDate() {
			return original_release_date;
		}
		public String getName() {
			return name;
		}
		public String getResourceType() {
			return resource_type;
		}
	}

	private static class DateTimeTypeConverter implements JsonSerializer<DateTime>, JsonDeserializer<DateTime> {
		@Override
		public JsonElement serialize(DateTime src, Type srcType, JsonSerializationContext context) {
			return new JsonPrimitive(src.toString());
		}

		@Override
		public DateTime deserialize(JsonElement json, Type type, JsonDeserializationContext context)
		throws JsonParseException {
			try {
				SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				return new DateTime(timeFormat.parseObject(json.getAsString()));
			} catch (IllegalArgumentException e) {
				// May be it came in formatted as a java.util.Date, so try that
				Date date = context.deserialize(json, Date.class);
				return new DateTime(date);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
}

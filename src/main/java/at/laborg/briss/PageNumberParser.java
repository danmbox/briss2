package at.laborg.briss;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageNumberParser {

	/**
	 * Super simple page-number parser. It handles entries like: "1-2;34;3-16"
	 * 
	 * @param input
	 *            String to be parsed.
	 * @return
	 * @throws ParseException
	 */
	public static Set<Integer> parsePageNumber(String input) throws ParseException {
		
		Pattern p = Pattern.compile("[^0-9-;]");
		Matcher m = p.matcher(input);

		if (m.find()) {
			throw new ParseException("Allowed characters: \"0-9\" \";\" \"-\" ",0);
		}
		
		// now tokenize by ;
		StringTokenizer tokenizer = new StringTokenizer(input,";");
		
		
		Set<Integer> pNS = new HashSet<Integer>();
		while (tokenizer.hasMoreElements()) {
			 pNS.addAll(extractPageNumbers(tokenizer.nextToken()));
		}

		return pNS;
	}

	private static Set<Integer> extractPageNumbers(String input) throws ParseException {

		StringTokenizer tokenizer = new StringTokenizer(input, "-");
		Set<Integer> returnSet = new HashSet<Integer>();
		if (tokenizer.countTokens() == 1) {
			// it's only a number, lets parse it
			Integer pageNumber = Integer.parseInt(input);
			returnSet.add(pageNumber);
			return returnSet;
		} else if (tokenizer.countTokens() == 2) {
			int start = Integer.parseInt(tokenizer.nextToken());
			int end = Integer.parseInt(tokenizer.nextToken());
			if (start > end) {
				throw new ParseException("End must be bigger than start in \""+ input +"\"", 0);
			} else {
				for (int i = start; i <= end;i++) {
					returnSet.add(i);
				}
				return returnSet;
			}
		} else {
			throw new ParseException("\"" + input
					+ "\" has to many - characters!", 0);
		}
	}
}

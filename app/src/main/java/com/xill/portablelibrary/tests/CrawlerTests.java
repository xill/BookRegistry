package com.xill.portablelibrary.tests;

import com.xill.portablelibrary.Crawler.EntryObject;
import com.xill.portablelibrary.Crawler.WorldCatCrawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by Sami on 1/21/2018.
 */

public class CrawlerTests {

	private static String wcTestData = "<td class=\"result details\">\n" +
			"    <div class=\"oclc_number\">982489191</div>\n" +
			"    <div class=\"item_number\">1</div>\n" +
			"<div class=\"name\">\n" +
			"   <a id=\"result-1\" href=\"/title/fujori-na-atashitachi/oclc/982489191&referer=brief_results\"><strong><div class=vernacular lang=\"ja\">不条理なあたし達 /</div>\n" +
			"Fujōri na atashitachi</strong></a>\n" +
			"     </div>\n" +
			"\n" +
			"<div class=\"author\">by <span class=vernacular lang=\"ja\">竹宮ジン.</span> Jin Takemiya</div><div class=\"type\">\n" +
			"            <img class='icn' src='http://static1.worldcat.org/wcpa/rel20171212/images/icon-bks.gif' alt=' ' height='16' width='16' >&nbsp;<span class='itemType'>Print book</span><a href=\"/title/fujori-na-atashitachi/oclc/982489191/editions?editionsView=true&referer=br\"\n" +
			"                       title=\"View all held editions and formats for this item\"> View all formats and languages &raquo;</a>\n" +
			"                </div>\n" +
			"<div class=\"type language\">Language: <span class=\"itemLanguage\">Japanese</span> &nbsp;</div><div class=\"publisher\">Publisher: <span class=\"itemPublisher\"><span class=vernacular lang=\"ja\">白泉社,</span> Tōkyō : Hakusensha, 2017.</span></div><!-- collection: /z-wcorg/ -->\n" +
			"<ul class=\"options\">\n" +
			"  <li class=\"elinkload\">Checking...</li>\n" +
			"   <li class=\"elinkstatus av\" id=\"et1\" style=\"display: none;\"><a class=\"tab\" id=\"el1\" href=\"javascript:void(0);\" title=\"View this item\">View Now</a></li>\n" +
			"\n" +
			"<li> <a href=\"/title/fujori-na-atashitachi/oclc/982489191/editions?editionsView=true&referer=br\" title=\"View all held editions and formats for this item\"> View all editions &raquo;</a></li>\n" +
			"        </ul>\n" +
			"\n" +
			"\t<div class=\"panel hidepanel\" id=\"elpanel1\"><p class=\"closepanel\"><a href=\"javascript:void(0);\" title=\"Close\">Close</a></p></div>\n" +
			" <div id=\"slice\">\n" +
			"        <span class=\"Z3988\" title=\"url_ver=Z39.88-2004&rft_val_fmt=info%3Aofi%2Ffmt%3Akev%3Amtx%3Abook&rft.genre=book&req_dat=%3Csessionid%3E&rfe_dat=%3Caccessionnumber%3E982489191%3C%2Faccessionnumber%3E&rft_id=info%3Aoclcnum%2F982489191&rft_id=urn%3AISBN%3A9784592711155&rft.aulast=Takemiya&rft.aufirst=Jin&rft.btitle=Fujo%CC%84ri+na+atashitachi&rft.date=2017&rft.isbn=9784592711155&rft.place=To%CC%84kyo%CC%84&rft.pub=Hakusensha&rft.genre=book&rft_dat=%7B%22stdrt1%22%3A%22Book%22%2C%22stdrt2%22%3A%22PrintBook%22%7D\"></span>\n" +
			"</div>\n" +
			"</td>";

	private static String wcTestData2 = "<td class=\"result details\">\n" +
			"    <div class=\"oclc_number\">756799994</div>\n" +
			"    <div class=\"item_number\">1</div>\n" +
			"<div class=\"name\">\n" +
			"   <a id=\"result-1\" href=\"/title/certain-scientific-railgun-1/oclc/756799994&referer=brief_results\"><strong>A Certain Scientific Railgun 1.</strong></a>\n" +
			"     </div>\n" +
			"\n" +
			"<div class=\"author\">by Kazuma Kamachi; Motoi Fuyukawa</div><div class=\"type\">\n" +
			"            <img class='icn' src='http://static1.worldcat.org/wcpa/rel20180125/images/icon-bks.gif' alt=' ' height='16' width='16' >&nbsp;<span class='itemType'>Print book</span><a href=\"/title/certain-scientific-railgun-1/oclc/756799994/editions?editionsView=true&referer=br\"\n" +
			"                       title=\"View all held editions and formats for this item\"> View all formats and languages &raquo;</a>\n" +
			"                </div>\n" +
			"<div class=\"type language\">Language: <span class=\"itemLanguage\">English</span> &nbsp;</div><div class=\"publisher\">Publisher: <span class=\"itemPublisher\">Seven Seas Entertainment Llc 2011.</span></div><!-- collection: /z-wcorg/ -->\n" +
			"<ul class=\"options\">\n" +
			"  <li class=\"elinkload\">Checking...</li>\n" +
			"   <li class=\"elinkstatus av\" id=\"et1\" style=\"display: none;\"><a class=\"tab\" id=\"el1\" href=\"javascript:void(0);\" title=\"View this item\">View Now</a></li>\n" +
			"\n" +
			"<li> <a href=\"/title/certain-scientific-railgun-1/oclc/756799994/editions?editionsView=true&referer=br\" title=\"View all held editions and formats for this item\"> View all editions &raquo;</a></li>\n" +
			"        </ul>\n" +
			"\n" +
			"\t<div class=\"panel hidepanel\" id=\"elpanel1\"><p class=\"closepanel\"><a href=\"javascript:void(0);\" title=\"Close\">Close</a></p></div>\n" +
			" <div id=\"slice\">\n" +
			"        <span class=\"Z3988\" title=\"url_ver=Z39.88-2004&rft_val_fmt=info%3Aofi%2Ffmt%3Akev%3Amtx%3Abook&rft.genre=book&req_dat=%3Csessionid%3E&rfe_dat=%3Caccessionnumber%3E756799994%3C%2Faccessionnumber%3E&rft_id=info%3Aoclcnum%2F756799994&rft_id=urn%3AISBN%3A9781935934004&rft.aulast=Kamachi&rft.aufirst=Kazuma&rft.btitle=A+Certain+Scientific+Railgun+1.&rft.date=2011&rft.isbn=9781935934004&rft.pub=Seven+Seas+Entertainment+Llc&rft.genre=book&rft_dat=%7B%22stdrt1%22%3A%22Book%22%2C%22stdrt2%22%3A%22PrintBook%22%7D\"></span>\n" +
			"</div>\n" +
			"</td>";

	public static boolean isLatinLetter(char c) {
		return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
	}

	public static void main(String[] argv) {

		String name = "アフターアワーズ = AFTER HOURS. 3";
		//String name = "Afuta awazu. 3.";
		System.out.println(name);
		System.out.println("result " + (isLatinLetter(name.charAt(0))));

		//runOnlineTests();
		//runOfflineTests();
	}

	public static void runOnlineTests() {
		// http://www.worldcat.org/search?q=9781935934004&qt=results_page
		// http://www.worldcat.org/search?q=9784592711155&qt=results_page

		long perItemDelayMS = 5000;
		String wcatPattern = "http://www.worldcat.org/search?q=<isbn>&qt=results_page";
		String[] isbn = {
			"9781626926844", // Bloom into you, vol. 4 // incorrect lining
			"9781945054679", // Flying witch 5
			"9781935934004", // railgun vol 1
			"9784592711155", // Fujōri na atashitachi
			"9784592710592", // apartmate. 1
			"9789521610349", // emma vol 5?
			"9781421592985", // Sweet blue flowers. Part one.
			"9780316343657", // prison school vol 1
			"9780316346139", // prison school vol 3 // no vol number
			"9784091894731", // yuzumorisan vol 2
			"9784593881529", // odette vol 4
			"9784091867285", // koi ha ameagari no youni vol 1
			"9784758078252", // kimi ha shoujo // no entry ??
			"9781632366306", // vinland saga 10
			"9781945054877", // my boy 1 // no vol number
			"9784785954239", // mebae vol 3
			"9784063409505", // koiiji vol 1
			"9784091898364", // after hours 3
			"9781427816719", // maria holic 1
			"9780316314763", // spice & wolf 12 // no vol number
			"9781945054785", // city 1
			"9781421506180", // claymore 1 // no vol number
			"9781595327710", // beck 1 // no vol number
			"9784758071949", // hatsukoi...
			"9784048937054", // yotsubato 14
			"9784040665566", // shiroyuri 1
			"9784088707020", // one punch-man 2 // no vol number
			"9784785955182", // juu jyou futama kanojo 2
			"9784758008464", // wotaku no koi ha muzukashii 1
			"9784758072649", // citrus 1
			"9784048937757", // ano ko to me ga au tabi watashi ha
			"9784040696904" // yuri kagi
		};

		List<String> responses = new ArrayList<String>(isbn.length);
		for(String isbnCode : isbn) {
			String resp = null;
			boolean hasCache = false;
			String cacheFilePath = "TEST_DATA/worldcat/" + isbnCode + ".txt";
			System.out.println("processing " + isbnCode);

			try {
				resp = readStringFromFile(cacheFilePath);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(resp != null) {
				System.out.println("Found in cache.");
				hasCache = true;
			} else {
				System.out.println("Not in cache. fetching...");
				try {
					resp = readStringFromURL(wcatPattern.replace("<isbn>",isbnCode));
				} catch (IOException e) {
					e.printStackTrace();
				}

				// wait a bit after a fetch.
				try {
					Thread.sleep(perItemDelayMS + (long)(1000 - Math.random() * 2000));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			if(resp != null && resp.length() > 0) {
				responses.add(resp);

				if(!hasCache)
					writeStringToFile(cacheFilePath, resp);
			}
		}

		runTests(responses.toArray(new String[0]));
	}

	public static void runOfflineTests() {
		runTests(wcTestData, wcTestData2);
	}

	public static void runTests(String... entries) {
		WorldCatCrawler worldCatCrawler = new WorldCatCrawler();
		for(String entry : entries) {
			EntryObject result = worldCatCrawler.getResultEntries(entry);
			printResults(result);
			System.out.println("<><><><><><><><><><><>");
			System.out.println("");
		}
	}

	public static void printResults(EntryObject result) {
		String[] names = result.names;
		System.out.println("names");
		for(int i = 0; i < names.length; ++i) {
			System.out.println(names[i]);
		}
		System.out.println("==========");

		System.out.println("authors");
		String[] authors = result.authors;
		for(int i = 0; i < authors.length; ++i) {
			System.out.println(authors[i]);
		}
		System.out.println("==========");

		System.out.println("languages");
		String[] languages = result.languages;
		for(int i = 0; i < languages.length; ++i) {
			System.out.println(languages[i]);
		}
		System.out.println("==========");

		System.out.println("publishers");
		String[] publishers = result.publishers;
		for(int i = 0; i < publishers.length; ++i) {
			System.out.println(publishers[i]);
		}
		System.out.println("==========");

		System.out.println("thumbnail");
		System.out.println(result.thumbnail);
	}

	public static String readStringFromURL(String requestURL) throws IOException
	{
		try (Scanner scanner = new Scanner(new URL(requestURL).openStream(),
				StandardCharsets.UTF_8.toString()))
		{
			scanner.useDelimiter("\\A");
			return scanner.hasNext() ? scanner.next() : "";
		}
	}

	private static String readStringFromFile(String fileUrl) throws IOException {
		File file = new File(fileUrl);
		if(!file.exists()) return null;

		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		StringBuilder stringBuilder = new StringBuilder();
		String ls = System.getProperty("line.separator");

		try {
			while((line = reader.readLine()) != null) {
				stringBuilder.append(line);
				stringBuilder.append(ls);
			}

			return stringBuilder.toString();
		} finally {
			reader.close();
		}
	}

	private static void writeStringToFile(String fileUrl, String data) {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(fileUrl, "UTF-8");
			writer.println(data);
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
}

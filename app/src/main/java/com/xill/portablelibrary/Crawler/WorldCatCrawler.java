package com.xill.portablelibrary.Crawler;

import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Sami on 1/20/2018.
 */

public class WorldCatCrawler implements CrawlerInterface {

	public String[] getResultNames(Document doc) {

		//<div class="name">
		//<a id="result-1" href="/title/fujori-na-atashitachi/oclc/982489191&referer=brief_results">
		// 	<strong><div class=vernacular lang="ja">不条理なあたし達 </div>Fujōri na atashitachi</strong>
		//</a>
		//</div>

		//<div class="name">
		//<a id="result-1" href="/title/certain-scientific-railgun-1/oclc/756799994&referer=brief_results">
		// 	<strong>A Certain Scientific Railgun 1.</strong>
		//</a>
		//</div>


		Element elems = doc.select(".name strong").first();
		String[] results = null;
		if(elems != null) {
			results = elems.text().split("/");
			for(int i = 0; i < results.length; ++i) {
				results[i] = results[i].trim();
			}
		}

		return results != null ? results : new String[0];
	}

	public String[] getResultAuthors(Document doc) {

		// <div class="author">by <span class=vernacular lang="ja">竹宮ジン.</span> Jin Takemiya</div>
		// <div class="author">by Kazuma Kamachi; Motoi Fuyukawa</div>

		List<String> names = new ArrayList<String>();
		String rawText = "";
		Element elems = doc.select(".author").first();
		Elements nameSpans = null;
		if(elems != null) {
			nameSpans = elems.select("span");
			rawText = elems.text();
		}

		if(rawText.startsWith("by "))
			rawText = rawText.substring(3,rawText.length());

		if(nameSpans != null) {
			for(Element span : nameSpans) {

				String name = span.text();
				// remove span name from raw string.
				if(rawText.contains(name))
					rawText = rawText.replace(name,"");

				// remove pointless dot after the name if present.
				if(name.endsWith("."))
					name = name.substring(0,name.length()-1);
				name = name.trim();
				// only add if theres something to add.
				if(name.length() > 0)
					names.add(name);
			}
		}

		rawText = rawText.trim();
		if(rawText.length() > 0)
			names.add(rawText);

		// process names which need splitting.
		int len = names.size();
		for(int i = len-1; i >= 0; --i) {
			if(names.get(i).contains(";")) {
				String[] nnames = names.get(i).split(";");
				for(String nname : nnames) {
					nname = nname.trim();
					if(nname.length() > 0)
						names.add(nname);
				}
				names.remove(i);
			}
		}

		String[] results = names.toArray(new String[0]);
		return results;
	}

	public String[] getResultLangs(Document doc) {

		// <span class="itemLanguage">Japanese</span>
		// <span class="itemLanguage">English</span>

		Element first = doc.select(".itemLanguage").first();
		String[] results = null;
		if(first != null) results = new String[]{ first.text().trim() };
		return results != null ? results : new String[0];
	}

	public String[] getResultPublishers(Document doc) {

		// <span class="itemPublisher"><span class=vernacular lang="ja">白泉社,</span> Tōkyō : Hakusensha, 2017.</span>
		// <span class="itemPublisher">Seven Seas Entertainment Llc 2011.</span>

		Element first = doc.select(".itemPublisher").first();
		String[] results = null;
		if(first != null) results = new String[]{ first.text().trim() };
		return results != null ? results : new String[0];
	}

	public String getThumbnail(Document doc) {

		Element first = doc.select(".menuElem .coverart").first();
		String result = "";
		if(first != null) {
			Element img = first.getElementsByTag("img").first();
			if(img != null) {
				result = img.attr("src");
			}
		}

		return result;
	}

	public String[] removeDuplicates(String[] results) {
		List<String> items = new ArrayList<String>(Arrays.asList(results));
		int len = items.size();
		for (int i = len-1 ; i >= 0 ; --i) {
			String item = items.get(i);
			for(int f = i-1; f >= 0; --f) {
				if(item.equals(items.get(f))) {
					items.remove(i);
					break;
				}
			}
		}

		return items.toArray(new String[0]);
	}

	public EntryObject getResultEntries(String data) {
		Document doc = Jsoup.parse(data);
		EntryObject entries = new EntryObject();
		entries.names = removeDuplicates(getResultNames(doc));
		entries.authors = removeDuplicates(getResultAuthors(doc));
		entries.languages = removeDuplicates(getResultLangs(doc));
		entries.publishers = removeDuplicates(getResultPublishers(doc));
		entries.thumbnail = getThumbnail(doc);

		return entries;
	}
}

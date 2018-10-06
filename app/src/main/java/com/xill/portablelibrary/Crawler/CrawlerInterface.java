package com.xill.portablelibrary.Crawler;

/**
 * Created by Sami on 1/20/2018.
 */

public interface CrawlerInterface {

	/**
	 * Parse given data for entries.
	 *
	 * @param data - data to parse.
	 * @return - parsed data in an EntryObject.
	 */
	public EntryObject getResultEntries(String data);
}

/*
 * Copyright (C) 2026 Sounak Choudhury
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.sounaks.indiangold.rates;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The rules of a robots.txt file that apply to this app (RFC 9309): the group naming our product token, otherwise the
 * "*" group. The longest matching rule wins, and Allow wins a tie.
 * @author Sounak Choudhury
 */
final class RobotsTxt
{
	private record Rule(boolean allow, String path, Pattern pattern)
	{
	}

	private final List<Rule> rules;

	private RobotsTxt(List<Rule> rules)
	{
		this.rules = rules;
	}

	static RobotsTxt allowAll()
	{
		return new RobotsTxt(List.of());
	}

	/**
	 * Parses robots.txt.
	 * @param text The file's content.
	 * @param userAgent Our User-Agent header; its product token (before "/") selects a group.
	 * @return The rules for us.
	 */
	static RobotsTxt parse(String text, String userAgent)
	{
		String product = userAgent.split("[/ ]", 2)[0].toLowerCase(Locale.ROOT);
		List<Rule> ours = new ArrayList<>(), anyone = new ArrayList<>();
		List<String> groupAgents = new ArrayList<>();
		List<Rule> groupRules = new ArrayList<>();
		boolean inRules = false;
		boolean foundOurs = false;
		for(String raw : (text + "\nuser-agent: end-of-file-marker").split("\\R"))
		{
			String line = raw.replaceAll("#.*", "").trim();
			int colon = line.indexOf(':');
			if(colon < 0) continue;
			String field = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
			String value = line.substring(colon + 1).trim();
			if(field.equals("user-agent"))
			{
				if(inRules)
				{
					// a new group starts: file away the finished one
					if(groupAgents.contains(product)) { ours.addAll(groupRules); foundOurs = true; }
					if(groupAgents.contains("*")) anyone.addAll(groupRules);
					groupAgents.clear();
					groupRules.clear();
					inRules = false;
				}
				groupAgents.add(value.toLowerCase(Locale.ROOT));
			}
			else if(field.equals("allow") || field.equals("disallow"))
			{
				inRules = true;
				if(!value.isEmpty()) groupRules.add(new Rule(field.equals("allow"), value, toPattern(value)));
			}
		}
		return new RobotsTxt(foundOurs ? ours : anyone);
	}

	private static Pattern toPattern(String path)
	{
		boolean anchored = path.endsWith("$");
		String body = anchored ? path.substring(0, path.length() - 1) : path;
		StringBuilder regex = new StringBuilder();
		for(String part : body.split("\\*", -1))
		{
			if(regex.length() > 0) regex.append(".*");
			regex.append(Pattern.quote(part));
		}
		return Pattern.compile(regex + (anchored ? "" : ".*"), Pattern.DOTALL);
	}

	/**
	 * Checks a path.
	 * @param pathAndQuery The URL path with its query, e.g. "/en/markdaten.php".
	 * @return True if the rules allow fetching it.
	 */
	boolean allows(String pathAndQuery)
	{
		Rule best = null;
		for(Rule rule : rules)
		{
			if(!rule.pattern().matcher(pathAndQuery).matches()) continue;
			if(best == null || rule.path().length() > best.path().length() || (rule.path().length() == best.path().length() && rule.allow()))
				best = rule;
		}
		return best == null || best.allow();
	}
}

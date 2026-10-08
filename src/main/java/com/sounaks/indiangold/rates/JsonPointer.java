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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Optional;

/**
 * Finds a value in JSON by a JSON pointer (RFC 6901), e.g. "/metals/gold" or "/data/0/price".
 * @author Sounak Choudhury
 */
final class JsonPointer
{
	private JsonPointer()
	{
	}

	static Optional<JsonElement> find(JsonElement root, String pointer)
	{
		if(root == null || pointer == null) return Optional.empty();
		if(pointer.isEmpty() || pointer.equals("/")) return Optional.of(root);
		if(!pointer.startsWith("/")) pointer = "/" + pointer;
		JsonElement current = root;
		for(String raw : pointer.substring(1).split("/", -1))
		{
			String token = raw.replace("~1", "/").replace("~0", "~");
			if(current instanceof JsonObject object)
			{
				current = object.get(token);
			}
			else if(current instanceof JsonArray array)
			{
				try
				{
					int index = Integer.parseInt(token);
					current = index >= 0 && index < array.size() ? array.get(index) : null;
				}
				catch(NumberFormatException e)
				{
					return Optional.empty();
				}
			}
			else
			{
				return Optional.empty();
			}
			if(current == null || current.isJsonNull()) return Optional.empty();
		}
		return Optional.of(current);
	}
}

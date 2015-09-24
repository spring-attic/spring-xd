/*
 * Copyright 2015 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.test.domain;

import java.util.Date;
import java.util.UUID;

import org.springframework.data.cassandra.mapping.PrimaryKey;
import org.springframework.data.cassandra.mapping.Table;

/**
 * Test POJO
 * @author David Webb
 */
@Table("book")
public class Book {

	@PrimaryKey
	private UUID isbn;

	private String title;

	private String author;

	private int pages;

	private Date saleDate;

	private boolean inStock;

	public Book() {
	}

	public Book(UUID isbn, String title, String author) {
		this.isbn = isbn;
		this.title = title;
		this.author = author;
	}

	/**
	 * @return Returns the isbn.
	 */
	public UUID getIsbn() {
		return isbn;
	}

	/**
	 * @return Returns the saleDate.
	 */
	public Date getSaleDate() {
		return saleDate;
	}

	/**
	 * @param saleDate The saleDate to set.
	 */
	public void setSaleDate(Date saleDate) {
		this.saleDate = saleDate;
	}

	/**
	 * @return Returns the inStock.
	 */
	public boolean isInStock() {
		return this.inStock;
	}

	/**
	 * @param inStock The isInStock to set.
	 */
	public void setInStock(boolean inStock) {
		this.inStock = inStock;
	}

	/**
	 * @param isbn The isbn to set.
	 */
	public void setIsbn(UUID isbn) {
		this.isbn = isbn;
	}

	/**
	 * @return Returns the title.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title The title to set.
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return Returns the author.
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * @param author The author to set.
	 */
	public void setAuthor(String author) {
		this.author = author;
	}

	/**
	 * @return Returns the pages.
	 */
	public int getPages() {
		return pages;
	}

	/**
	 * @param pages The pages to set.
	 */
	public void setPages(int pages) {
		this.pages = pages;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return ("isbn -> " + isbn) + "\n" + "tile -> " + title + "\n" + "author -> " + author
				+ "\n" + "pages -> " + pages + "\n";
	}

}

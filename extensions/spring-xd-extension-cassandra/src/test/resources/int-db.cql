DROP TABLE IF EXISTS book;

CREATE TABLE book  (
	isbn    	uuid PRIMARY KEY,
	author  	text,
	instock 	boolean,
	pages   	int,
	saledate	timestamp,
	title   	text
);


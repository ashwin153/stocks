SET SESSION sql_mode='ALLOW_INVALID_DATES';
SET foreign_key_checks = 0;

LOAD DATA LOCAL INFILE '/Users/ashwin/Downloads/2012q4/sub.txt'
IGNORE INTO TABLE registrants 
	FIELDS TERMINATED BY '\t'
	LINES TERMINATED BY '\n'
	IGNORE 1 LINES
(@dummy, cik, name, @sic, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, 
@dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @ein, @dummy, @dummy, @dummy, @dummy, @dummy, 
@dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy)
SET
	sic = nullif(@sic, ''),
	ein = nullif(@ein, '');

LOAD DATA LOCAL INFILE '/Users/ashwin/Downloads/2012q4/sub.txt'
IGNORE INTO TABLE submissions 
	FIELDS TERMINATED BY '\t'
	LINES TERMINATED BY '\n'
	IGNORE 1 LINES
(adsh, cik, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, 
@dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @afs, @wksi, @fye, form, 
@period, @fy, @fp, @filed, @accepted, @dummy, @detail, @dummy, @dummy, @dummy)
SET 
	afs = cast(SUBSTRING(nullif(@afs, ''), 1, 1) as signed) - 1,
	wksi = cast(@wksi as signed),
	period = STR_TO_DATE(@period, '%Y%m%d'),
	fye = STR_TO_DATE(nullif(CONCAT(@fy, @fye), ''), '%Y%m%d'),
	fp = nullif(@fp, ''),
	filed = STR_TO_DATE(@filed, '%Y%m%d'),
	accepted = STR_TO_DATE(@accepted, '%Y-%m-%d %T.%f'),
	detail = cast(@detail as signed);

LOAD DATA LOCAL INFILE '/Users/ashwin/Downloads/2012q4/tag.txt'
IGNORE INTO TABLE tags 
	FIELDS TERMINATED BY '\t'
	LINES TERMINATED BY '\n'
	IGNORE 1 LINES
(name, version, @custom, @abstract, @datatype, @iord, @crdr, @label, @foc)
SET 
	custom = cast(@custom as signed),
	abstract = cast(@abstract as signed),
	datatype = nullif(@datatype, ''),
	iord = nullif(@iord, ''),
	crdr = nullif(@crdr, ''),
	label = nullif(@label, ''),
	foc = nullif(@foc, '');

LOAD DATA LOCAL INFILE '/Users/ashwin/Downloads/2012q4/num.txt'
IGNORE INTO TABLE numbers 
	FIELDS TERMINATED BY '\t'
	LINES TERMINATED BY '\n'
	IGNORE 1 LINES
(adsh, name, version, coreg, @ddate, duration, units, @value, @footnote)
SET 
	ddate = STR_TO_DATE(@ddate, '%Y%m%d'),
	value = nullif(@value, ''),
	footnote = nullif(@footnote, '');
SET foreign_key_checks = 1;

SELECT TABLE_NAME, TABLE_ROWS FROM `information_schema`.`tables` WHERE `table_schema` = 'sec';
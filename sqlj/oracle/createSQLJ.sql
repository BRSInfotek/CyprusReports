/**
 *	Create SQL Java Functions (Oracle)
 *
 *	Author + Copyright 1999-2005 Anshul
 *	$Header: /cvs/cyprus/sqlj/oracle/createSQLJ.sql,v 1.1 2006/04/21 18:04:47 Anshul Exp $
 */
 
CREATE OR REPLACE FUNCTION adempiereVersion
 	RETURN VARCHAR2
 	AS LANGUAGE JAVA 
	NAME 'org.cyprusbrs.sqlj.Cyprus.getVersion() return java.lang.String';
/
CREATE OR REPLACE FUNCTION adempiereProperties
 	RETURN VARCHAR2
 	AS LANGUAGE JAVA 
	NAME 'org.cyprusbrs.sqlj.Cyprus.getProperties() return java.lang.String';
/
CREATE OR REPLACE FUNCTION adempiereProperty(p_key VARCHAR2)
 	RETURN VARCHAR2
 	AS LANGUAGE JAVA 
	NAME 'org.cyprusbrs.sqlj.Cyprus.getProperty(java.lang.String) return java.lang.String';
/
CREATE OR REPLACE FUNCTION get_Sysconfig(Name VARCHAR2, defaultValue VARCHAR2, AD_Client_ID NUMBER, AD_Org_ID NUMBER)
 	RETURN VARCHAR2
 	AS LANGUAGE JAVA 
	NAME 'org.cyprusbrs.sqlj.Cyprus.get_Sysconfig(java.lang.String,java.lang.String,int,int) return java.lang.String';
/



/** General	**/
BEGIN
	dbms_java.grant_permission('CYPRUS','SYS:java.util.PropertyPermission', '*', 'read,write');
END;
/

/** Get Character at Position   */
CREATE OR REPLACE FUNCTION charAt
(
    p_string    VARCHAR2,
    p_pos       NUMBER
)
 	RETURN VARCHAR2
AS
BEGIN
    RETURN SUBSTR(p_string, p_pos, 1);
END;    
/
/** GetDate                     */
CREATE OR REPLACE FUNCTION getdate
 	RETURN DATE
AS
BEGIN
    RETURN SysDate;
END;    
/
/** First Of DD/DY/MM/Q         */
CREATE OR REPLACE FUNCTION firstOf
(
    p_date      DATE,
    p_datePart  VARCHAR2
)
 	RETURN DATE
AS
BEGIN
    RETURN TRUNC(p_date, p_datePart);
END;    
/
/** Add Number of Days      */
CREATE OR REPLACE FUNCTION addDays
(
    p_date      DATE,
    p_days      NUMBER
)
 	RETURN DATE
AS
BEGIN
    RETURN TRUNC(p_date) + p_days;
END;    
/
/** Difference in Days      */
CREATE OR REPLACE FUNCTION daysBetween
(
    p_date1     DATE,
    p_date2     DATE
)
 	RETURN NUMBER
AS
BEGIN
    RETURN TRUNC(p_date1) - TRUNC(p_date2);
END;    
/


SELECT --adempiereVersion(), adempiereProperty('java.vendor'), 
    TRUNC(getdate()) FROM DUAL
/

EXIT

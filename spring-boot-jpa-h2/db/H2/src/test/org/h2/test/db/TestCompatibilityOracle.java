/*
 * Copyright 2004-2014 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.test.db;

import org.h2.test.TestBase;
import org.h2.tools.SimpleResultSet;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;

/**
 * Test Oracle compatibility mode.
 */
public class TestCompatibilityOracle extends TestBase {

    /**
     * Run just this test.
     *
     * @param s ignored
     */
    public static void main(String... s) throws Exception {
        TestBase test = TestBase.createCaller().init();
        test.test();
    }

    @Override
    public void test() throws Exception {
        testTreatEmptyStringsAsNull();
        testDecimalScale();
        testPoundSymbolInColumnName();
        testToDate();
        testForbidEmptyInClause();
    }

    private void testTreatEmptyStringsAsNull() throws SQLException {
        deleteDb("oracle");
        Connection conn = getConnection("oracle;MODE=Oracle");
        Statement stat = conn.createStatement();

        stat.execute("CREATE TABLE A (ID NUMBER, X VARCHAR2(1))");
        stat.execute("INSERT INTO A VALUES (1, 'a')");
        stat.execute("INSERT INTO A VALUES (2, '')");
        stat.execute("INSERT INTO A VALUES (3, ' ')");
        assertResult("3", stat, "SELECT COUNT(*) FROM A");
        assertResult("1", stat, "SELECT COUNT(*) FROM A WHERE X IS NULL");
        assertResult("2", stat, "SELECT COUNT(*) FROM A WHERE TRIM(X) IS NULL");
        assertResult("0", stat, "SELECT COUNT(*) FROM A WHERE X = ''");
        assertResult(new Object[][] { { 1, "a" }, { 2, null }, { 3, " " } },
                stat, "SELECT * FROM A");
        assertResult(new Object[][] { { 1, "a" }, { 2, null }, { 3, null } },
                stat, "SELECT ID, TRIM(X) FROM A");

        stat.execute("CREATE TABLE B (ID NUMBER, X NUMBER)");
        stat.execute("INSERT INTO B VALUES (1, '5')");
        stat.execute("INSERT INTO B VALUES (2, '')");
        assertResult("2", stat, "SELECT COUNT(*) FROM B");
        assertResult("1", stat, "SELECT COUNT(*) FROM B WHERE X IS NULL");
        assertResult("0", stat, "SELECT COUNT(*) FROM B WHERE X = ''");
        assertResult(new Object[][] { { 1, 5 }, { 2, null } },
                stat, "SELECT * FROM B");

        stat.execute("CREATE TABLE C (ID NUMBER, X TIMESTAMP)");
        stat.execute("INSERT INTO C VALUES (1, '1979-11-12')");
        stat.execute("INSERT INTO C VALUES (2, '')");
        assertResult("2", stat, "SELECT COUNT(*) FROM C");
        assertResult("1", stat, "SELECT COUNT(*) FROM C WHERE X IS NULL");
        assertResult("0", stat, "SELECT COUNT(*) FROM C WHERE X = ''");
        assertResult(new Object[][] { { 1, "1979-11-12 00:00:00.0" }, { 2, null } },
                stat, "SELECT * FROM C");

        stat.execute("CREATE TABLE D (ID NUMBER, X VARCHAR2(1))");
        stat.execute("INSERT INTO D VALUES (1, 'a')");
        stat.execute("SET @FOO = ''");
        stat.execute("INSERT INTO D VALUES (2, @FOO)");
        assertResult("2", stat, "SELECT COUNT(*) FROM D");
        assertResult("1", stat, "SELECT COUNT(*) FROM D WHERE X IS NULL");
        assertResult("0", stat, "SELECT COUNT(*) FROM D WHERE X = ''");
        assertResult(new Object[][] { { 1, "a" }, { 2, null } },
                stat, "SELECT * FROM D");

        stat.execute("CREATE TABLE E (ID NUMBER, X RAW(1))");
        stat.execute("INSERT INTO E VALUES (1, '0A')");
        stat.execute("INSERT INTO E VALUES (2, '')");
        assertResult("2", stat, "SELECT COUNT(*) FROM E");
        assertResult("1", stat, "SELECT COUNT(*) FROM E WHERE X IS NULL");
        assertResult("0", stat, "SELECT COUNT(*) FROM E WHERE X = ''");
        assertResult(new Object[][] { { 1, new byte[] { 10 } }, { 2, null } },
                stat, "SELECT * FROM E");

        stat.execute("CREATE TABLE F (ID NUMBER, X VARCHAR2(1))");
        stat.execute("INSERT INTO F VALUES (1, 'a')");
        PreparedStatement prep = conn.prepareStatement(
                "INSERT INTO F VALUES (2, ?)");
        prep.setString(1, "");
        prep.execute();
        assertResult("2", stat, "SELECT COUNT(*) FROM F");
        assertResult("1", stat, "SELECT COUNT(*) FROM F WHERE X IS NULL");
        assertResult("0", stat, "SELECT COUNT(*) FROM F WHERE X = ''");
        assertResult(new Object[][]{{1, "a"}, {2, null}}, stat, "SELECT * FROM F");

        conn.close();
    }

    private void testDecimalScale() throws SQLException {
        deleteDb("oracle");
        Connection conn = getConnection("oracle;MODE=Oracle");
        Statement stat = conn.createStatement();

        stat.execute("CREATE TABLE A (ID NUMBER, X DECIMAL(9,5))");
        stat.execute("INSERT INTO A VALUES (1, 2)");
        stat.execute("INSERT INTO A VALUES (2, 4.3)");
        stat.execute("INSERT INTO A VALUES (3, '6.78')");
        assertResult("3", stat, "SELECT COUNT(*) FROM A");
        assertResult(new Object[][] { { 1, 2 }, { 2, 4.3 }, { 3, 6.78 } },
                stat, "SELECT * FROM A");

        conn.close();
    }

    /**
     * Test the # in a column name for oracle compatibility
     */
    private void testPoundSymbolInColumnName() throws SQLException {
        deleteDb("oracle");
        Connection conn = getConnection("oracle;MODE=Oracle");
        Statement stat = conn.createStatement();

        stat.execute(
                "CREATE TABLE TEST(ID INT PRIMARY KEY, U##NAME VARCHAR(255))");
        stat.execute(
                "INSERT INTO TEST VALUES(1, 'Hello'), (2, 'HelloWorld'), (3, 'HelloWorldWorld')");

        assertResult("1", stat, "SELECT ID FROM TEST where U##NAME ='Hello'");

        conn.close();
    }

    private void testToDate() throws SQLException {
        if (Locale.getDefault() != Locale.ENGLISH) {
            return;
        }
        deleteDb("oracle");
        Connection conn = getConnection("oracle;MODE=Oracle");
        Statement stat = conn.createStatement();

        stat.execute("CREATE TABLE DATE_TABLE (ID NUMBER PRIMARY KEY, TEST_VAL TIMESTAMP)");
        stat.execute("INSERT INTO DATE_TABLE VALUES (1, " +
                "to_date('31-DEC-9999 23:59:59','DD-MON-RRRR HH24:MI:SS'))");
        stat.execute("INSERT INTO DATE_TABLE VALUES (2, " +
                "to_date('01-JAN-0001 00:00:00','DD-MON-RRRR HH24:MI:SS'))");

        assertResultDate("9999-12-31T23:59:59", stat,
                "SELECT TEST_VAL FROM DATE_TABLE WHERE ID=1");
        assertResultDate("0001-01-01T00:00:00", stat,
                "SELECT TEST_VAL FROM DATE_TABLE WHERE ID=2");

        conn.close();
    }

    private void testForbidEmptyInClause() throws SQLException {
        deleteDb("oracle");
        Connection conn = getConnection("oracle;MODE=Oracle");
        Statement stat = conn.createStatement();

        stat.execute("CREATE TABLE A (ID NUMBER, X VARCHAR2(1))");
        try {
            stat.executeQuery("SELECT * FROM A WHERE ID IN ()");
            fail();
        } catch (SQLException e) {
            // expected
        } finally {
            conn.close();
        }
    }

    private void assertResultDate(String expected, Statement stat, String sql)
            throws SQLException {
        SimpleDateFormat iso8601 = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss");
        ResultSet rs = stat.executeQuery(sql);
        if (rs.next()) {
            assertEquals(expected, iso8601.format(rs.getTimestamp(1)));
        } else {
            assertEquals(expected, null);
        }
    }

    private void assertResult(Object[][] expectedRowsOfValues, Statement stat,
            String sql) throws SQLException {
        assertResult(newSimpleResultSet(expectedRowsOfValues), stat, sql);
    }

    private void assertResult(ResultSet expected, Statement stat, String sql)
            throws SQLException {
        ResultSet actual = stat.executeQuery(sql);
        int expectedColumnCount = expected.getMetaData().getColumnCount();
        assertEquals(expectedColumnCount, actual.getMetaData().getColumnCount());
        while (true) {
            boolean expectedNext = expected.next();
            boolean actualNext = actual.next();
            if (!expectedNext && !actualNext) {
                return;
            }
            if (expectedNext != actualNext) {
                fail("number of rows in actual and expected results sets does not match");
            }
            for (int i = 0; i < expectedColumnCount; i++) {
                String expectedString = columnResultToString(expected.getObject(i + 1));
                String actualString = columnResultToString(actual.getObject(i + 1));
                assertEquals(expectedString, actualString);
            }
        }
    }

    private static String columnResultToString(Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof Object[]) {
            return Arrays.deepToString((Object[]) object);
        }
        if (object instanceof byte[]) {
            return Arrays.toString((byte[]) object);
        }
        return object.toString();
    }

    private static SimpleResultSet newSimpleResultSet(Object[][] rowsOfValues) {
        SimpleResultSet result = new SimpleResultSet();
        for (int i = 0; i < rowsOfValues[0].length; i++) {
            result.addColumn(i + "", Types.JAVA_OBJECT, 0, 0);
        }
        for (int i = 0; i < rowsOfValues.length; i++) {
            result.addRow(rowsOfValues[i]);
        }
        return result;
    }

}

/*
 * LICENSED UNDER THE LGPL 2.1,
 * http://www.gnu.org/licenses/lgpl.html
 * Also, the author promises to never sue IBM for any use of this file.
 *
 */

package net.jakubholy.dbunitexpress.exception.derby;

import java.sql.Connection;
import java.sql.Statement;

import net.jakubholy.dbunitexpress.AbstractEmbeddedDbTestCase;
import net.jakubholy.dbunitexpress.EmbeddedDbTester;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Simple verification of some of the possible errors.
 *
 */
public class DerbyExceptionInterpreterTest {

    private EmbeddedDbTester testDb = new EmbeddedDbTester();

	private DerbyExceptionInterpreter interpreter;

    @Before
	public void setUp() throws Exception {
		testDb.onSetup();
		interpreter = new DerbyExceptionInterpreter();
	}

    @Test
	public void testThatLockedTableDetected() throws Exception {

		// PREPARE
		final Connection connection = testDb.getSqlConnection();
		// Forbid autocommit so that lock will be retained until commit/rollback
		connection.setAutoCommit(false);
		try {
            connection.createStatement().execute("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.locks.waitTimeout', '1')");
			final Statement stmt = connection.createStatement();
			stmt.executeUpdate("UPDATE my_test_schema.my_test_table SET some_text='abc' where id=3");

			// TEST
			// Execute query - shall time out because of a write lock on the table
			try {
				testDb.getConnection().createQueryTable(
					"lockedTable", "select * from my_test_schema.my_test_table");
				fail("Should have failed because the table is locked due to an " +
						"uncommited update transaction.");
			} catch (Exception e) {
				final String explanation = interpreter.explain(e);
				assertEquals("The table is locked, perhaps your test code has " +
						"not cleaned correctly the DB resources that it used " +
						"(such as doing proper commit/rollback if it set autocommit " +
						"off).", explanation);
			}
		} finally {
			// CLEANUP !!!
			connection.commit();
			connection.setAutoCommit(true);
			connection.close();
			System.err.println(getClass() + ".testThatLockedTableDetected: " +
					"cleanup finished; conn.closed: " + connection.isClosed());
		}

	}

}

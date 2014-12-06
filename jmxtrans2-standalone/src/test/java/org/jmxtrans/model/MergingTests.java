package org.jmxtrans.model;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.jmxtrans.model.Server.mergeServerLists;
import static org.assertj.core.api.Assertions.assertThat;

public class MergingTests {

	@Test
	public void mergeAlreadyExistingServerDoesNotModifyList() throws ValidationException {
		List<Server> existingServers = new ArrayList<Server>();
		existingServers.add(ServerFixtures.createServerWithOneQuery("example.net", "123", "toto"));

		List<Server> newServers = new ArrayList<Server>();
		newServers.add(ServerFixtures.createServerWithOneQuery("example.net", "123", "toto"));

		List<Server> merged = mergeServerLists(existingServers, newServers);

		assertThat(merged).hasSize(1);

		Server mergedServer = merged.get(0);
		assertThat(mergedServer.getQueries()).hasSize(1);
	}

	@Test
	public void sameServerWithTwoDifferentQueriesMergesQueries() throws ValidationException {
		List<Server> existingServers = new ArrayList<Server>();
		existingServers.add(ServerFixtures.createServerWithOneQuery("example.net", "123", "toto"));

		List<Server> newServers = new ArrayList<Server>();
		newServers.add(ServerFixtures.createServerWithOneQuery("example.net", "123", "tutu"));

		List<Server> merged = mergeServerLists(existingServers, newServers);

		assertThat(merged).hasSize(1);
		Server mergedServer = merged.get(0);
		assertThat(mergedServer.getQueries()).hasSize(2);
	}


	@Test
	public void testMerge() throws Exception {
		Query q1 = Query.builder()
				.setObj("obj")
				.addAttr("foo")
				.addAttr("bar")
				.addKey("key1")
				.addKey("key2")
				.setResultAlias("alias")
				.build();

		// same as q1
		Query q2 = Query.builder()
				.setObj("obj")
				.addAttr("foo")
				.addAttr("bar")
				.addKey("key1")
				.addKey("key2")
				.setResultAlias("alias")
				.build();

		// different than q1 and q2
		Query q3 = Query.builder()
				.setObj("obj3")
				.addAttr("foo")
				.addAttr("bar")
				.addKey("key1")
				.addKey("key2")
				.setResultAlias("alias")
				.build();

		Server s1 = Server.builder()
				.setAlias("alias")
				.setHost("host")
				.setPort("8004")
				.setCronExpression("cron")
				.setNumQueryThreads(123)
				.setPassword("pass")
				.setUsername("user")
				.addQuery(q1)
				.addQuery(q2)
				.build();

		// same as s1
		Server s2 = Server.builder()
				.setAlias("alias")
				.setHost("host")
				.setPort("8004")
				.setCronExpression("cron")
				.setNumQueryThreads(123)
				.setPassword("pass")
				.setUsername("user")
				.addQuery(q1)
				.addQuery(q2)
				.build();

		Server s3 = Server.builder()
				.setAlias("alias")
				.setHost("host3")
				.setPort("8004")
				.setCronExpression("cron")
				.setNumQueryThreads(123)
				.setPassword("pass")
				.setUsername("user")
				.addQuery(q1)
				.addQuery(q2)
				.addQuery(q3)
				.build();

		List<Server> existing = new ArrayList<Server>();
		existing.add(s1);

		List<Server> adding = new ArrayList<Server>();

		adding.add(s2);
		existing = mergeServerLists(existing, adding);

		// should only have one server with 1 query since we just added the same
		// server and same query.
		assertThat(existing).hasSize(1);
		assertThat(existing.get(0).getQueries()).hasSize(1);

		adding.add(s3);
		existing = mergeServerLists(existing, adding);

		assertThat(existing).hasSize(2);
		assertThat(existing.get(0).getQueries()).hasSize(1);
		assertThat(existing.get(1).getQueries()).hasSize(2);
	}
}

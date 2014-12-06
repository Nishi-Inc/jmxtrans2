package org.jmxtrans.util;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.jmxtrans.model.JmxProcess;
import org.jmxtrans.model.Query;
import org.jmxtrans.model.Server;

import static com.google.common.collect.FluentIterable.from;
import static org.assertj.core.api.Assertions.assertThat;

public class JsonUtilsTest {

	@Test
	public void loadingFromFile() throws URISyntaxException, IOException {
		File input = new File(JsonUtilsTest.class.getResource("/example.json").toURI());

		JmxProcess process = JsonUtils.getJmxProcess(input);
		assertThat(process.getName()).isEqualTo("example.json");

		Server server = process.getServers().get(0);
		assertThat(server.getNumQueryThreads()).isEqualTo(2);

		Optional<Query> queryOptional = from(server.getQueries()).firstMatch(new ByObj("java.lang:type=Memory"));
		assertThat(queryOptional.isPresent()).isTrue();
		assertThat(queryOptional.get().getAttr().get(0)).isEqualTo("HeapMemoryUsage");
	}

	private static class ByObj implements Predicate<Query> {

		private final String obj;

		private ByObj(String obj) {
			this.obj = obj;
		}

		@Override
		public boolean apply(@Nullable Query query) {
			return query.getObj().equals(this.obj);
		}
	}
}

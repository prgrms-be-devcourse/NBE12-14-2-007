package com.team007.room_escape.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import tools.jackson.databind.JsonNode;

/**
 * API 표면(경로·파라미터·응답 스키마·필드명)을 고정한다.
 *
 * 코틀린 전환 중에 record 를 data class 로 바꾸다 보면 필드명이 바뀌거나
 * 빠지는 일이 생기는데, 상태 코드만 보는 테스트로는 잡히지 않는다.
 * 여기서는 OpenAPI 문서를 사람이 읽을 수 있는 형태로 줄여 기준 파일과 비교한다.
 *
 * 표면이 바뀌면 이 테스트가 깨진다. 의도한 변경이면 기준 파일을 다시 만들어
 * 커밋하고, 그 diff 를 PR 에서 사람이 확인한다.
 *
 *   ./gradlew test --tests "*ApiSurfaceTest" -Dapi.surface.update=true
 *
 * 인가(@PreAuthorize)는 OpenAPI 에 드러나지 않아 여기서 다루지 않는다.
 * 그쪽은 역할별 요청을 직접 보내는 각 *ApiContractTest 가 담당한다.
 */
class ApiSurfaceTest extends ApiContractSupport {

	private static final Path BASELINE = Path.of("src/test/resources/api-surface.txt");
	private static final Path ACTUAL = Path.of("build/api-surface-actual.txt");

	@Test
	void API_표면이_기준과_같다() throws Exception {
		String actual = buildSurface(fetchOpenApi());

		if (Boolean.getBoolean("api.surface.update")) {
			Files.createDirectories(BASELINE.getParent());
			Files.writeString(BASELINE, actual);
			return;
		}
		if (!Files.exists(BASELINE)) {
			writeActual(actual);
			throw new AssertionFailedError(
				"기준 파일이 없다. -Dapi.surface.update=true 로 만든 뒤 커밋해라: " + BASELINE);
		}

		String expected = Files.readString(BASELINE);
		if (!expected.equals(actual)) {
			writeActual(actual);
			throw new AssertionFailedError(
				"API 표면이 기준과 다르다. 의도한 변경이면 -Dapi.surface.update=true 로 기준을 갱신하고"
					+ " 그 diff 를 PR 에 포함해라.\n  기준: " + BASELINE + "\n  실제: " + ACTUAL
					+ "\n" + firstDifference(expected, actual));
		}
	}

	private JsonNode fetchOpenApi() throws Exception {
		var result = mvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andReturn();
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}

	/** 경로와 스키마를 이름순으로 정렬해, 순서가 흔들려도 diff 가 생기지 않게 한다. */
	private String buildSurface(JsonNode root) {
		StringBuilder out = new StringBuilder();

		out.append("# 엔드포인트\n");
		for (var path : sorted(root.get("paths")).entrySet()) {
			for (var method : sorted(path.getValue()).entrySet()) {
				JsonNode operation = method.getValue();
				out.append(method.getKey().toUpperCase()).append(' ').append(path.getKey()).append('\n');
				appendLine(out, "params", parameterNames(operation));
				appendLine(out, "body", List.of(schemaName(operation.path("requestBody")
					.path("content").path("application/json").path("schema"))));
				appendLine(out, "responses", responseSummaries(operation.path("responses")));
			}
		}

		out.append("\n# 스키마\n");
		for (var schema : sorted(root.path("components").path("schemas")).entrySet()) {
			out.append(schema.getKey()).append('\n');
			for (var property : sorted(schema.getValue().path("properties")).entrySet()) {
				out.append("  ").append(property.getKey()).append(": ")
					.append(typeOf(property.getValue())).append('\n');
			}
		}
		return out.toString();
	}

	private List<String> parameterNames(JsonNode operation) {
		List<String> names = new ArrayList<>();
		for (JsonNode parameter : operation.path("parameters")) {
			names.add(parameter.path("name").asString("?") + "(" + parameter.path("in").asString("?") + ")");
		}
		names.sort(String::compareTo);
		return names;
	}

	private List<String> responseSummaries(JsonNode responses) {
		List<String> summaries = new ArrayList<>();
		for (var response : sorted(responses).entrySet()) {
			summaries.add(response.getKey() + "->" + schemaName(response.getValue()
				.path("content").path("*/*").path("schema")));
		}
		return summaries;
	}

	/** $ref 는 스키마 이름만, 인라인 스키마는 타입만 남긴다. */
	private String schemaName(JsonNode schema) {
		if (schema.isMissingNode() || schema.isNull()) {
			return "-";
		}
		String ref = schema.path("$ref").asString("");
		if (!ref.isEmpty()) {
			return ref.substring(ref.lastIndexOf('/') + 1);
		}
		return typeOf(schema);
	}

	private String typeOf(JsonNode schema) {
		String ref = schema.path("$ref").asString("");
		if (!ref.isEmpty()) {
			return ref.substring(ref.lastIndexOf('/') + 1);
		}
		String type = schema.path("type").asString("object");
		if ("array".equals(type)) {
			return "array<" + typeOf(schema.path("items")) + ">";
		}
		String format = schema.path("format").asString("");
		return format.isEmpty() ? type : type + "/" + format;
	}

	private void appendLine(StringBuilder out, String label, List<String> values) {
		List<String> kept = values.stream().filter(v -> !"-".equals(v)).toList();
		if (!kept.isEmpty()) {
			out.append("  ").append(label).append(": ").append(String.join(", ", kept)).append('\n');
		}
	}

	private Map<String, JsonNode> sorted(JsonNode node) {
		Map<String, JsonNode> entries = new TreeMap<>();
		if (node != null && node.isObject()) {
			for (String name : node.propertyNames()) {
				entries.put(name, node.get(name));
			}
		}
		return entries;
	}

	/** 전체 diff 는 길어서, 처음 갈라지는 줄만 보여준다. */
	private String firstDifference(String expected, String actual) {
		String[] left = expected.split("\n", -1);
		String[] right = actual.split("\n", -1);
		for (int i = 0; i < Math.max(left.length, right.length); i++) {
			String l = i < left.length ? left[i] : "(없음)";
			String r = i < right.length ? right[i] : "(없음)";
			if (!l.equals(r)) {
				return "  처음 다른 곳 %d번째 줄\n    기준: %s\n    실제: %s".formatted(i + 1, l, r);
			}
		}
		return "";
	}

	private void writeActual(String actual) throws IOException {
		Files.createDirectories(ACTUAL.getParent());
		Files.writeString(ACTUAL, actual);
	}
}

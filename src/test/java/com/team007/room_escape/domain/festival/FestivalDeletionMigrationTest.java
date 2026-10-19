package com.team007.room_escape.domain.festival;

import static org.assertj.core.api.Assertions.assertThat;
import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

/** 독립 스키마에서 두 마이그레이션 경로를 함께 적용한다. 개발 데이터는 변경하지 않는다. */
class FestivalDeletionMigrationTest {
	@Test
	void migratesSubmissionOnlyDeletionWithoutGuessingActorAndRecalculatesGrade() throws Exception {
		String schema = "festival_migration_" + UUID.randomUUID().toString().replace("-", "");
		String url = "jdbc:postgresql://localhost:5432/roomescape_policy_test?currentSchema=" + schema + ",public";
		Flyway.configure().dataSource(url, "roomescape", "roomescape").schemas(schema)
			.locations("classpath:db/migration", "classpath:db/migration_v2").target("36").load().migrate();
		UUID owner = UUID.randomUUID();
		UUID history = UUID.randomUUID();
		long eventId;
		try (var connection = DriverManager.getConnection(url, "roomescape", "roomescape")) {
			try (var insert = connection.prepareStatement("""
				INSERT INTO member (id, email, password, role, nickname, created_at, updated_at)
				VALUES (?, 'migration@example.com', 'test', 'ROLE_TRUSTED', '이관 대상', NOW(), NOW())
				""")) {
				insert.setObject(1, owner);
				insert.executeUpdate();
			}
			try (var insert = connection.prepareStatement("""
				INSERT INTO festival (member_id, provider_type, begin_de, end_de, region, url, created_at, updated_at)
				VALUES (?, 'MEMBER', '2026-10-20', '2026-10-21', 'SEOUL', 'https://example.com/legacy', NOW(), NOW())
				RETURNING id
				""")) {
				insert.setObject(1, owner);
				try (var result = insert.executeQuery()) {
					result.next();
					eventId = result.getLong(1);
				}
			}
			try (var insert = connection.prepareStatement("""
				INSERT INTO festival_submission (id, festival_id, created_at, updated_at, deleted_at)
				VALUES (?, ?, NOW(), NOW(), '2026-10-10 12:00:00')
				""")) {
				insert.setObject(1, history);
				insert.setLong(2, eventId);
				insert.executeUpdate();
			}
		}
		Flyway.configure().dataSource(url, "roomescape", "roomescape").schemas(schema)
			.locations("classpath:db/migration", "classpath:db/migration_v2").target("37").load().migrate();
		try (var connection = DriverManager.getConnection(url, "roomescape", "roomescape");
			var select = connection.prepareStatement("""
				SELECT f.deleted_at, f.deletion_type, f.deleted_by, f.deletion_reason, m.role
				FROM festival f JOIN member m ON m.id = f.member_id WHERE f.id = ?
				""")) {
			select.setLong(1, eventId);
			try (var result = select.executeQuery()) {
				assertThat(result.next()).isTrue();
				assertThat(result.getTimestamp(1).toLocalDateTime().toString()).isEqualTo("2026-10-10T12:00");
				assertThat(result.getString(2)).isEqualTo("UNKNOWN");
				assertThat(result.getObject(3)).isNull();
				assertThat(result.getString(4)).contains("확인 불가");
				assertThat(result.getString(5)).isEqualTo("ROLE_UNVERIFIED");
			}
		}
		Flyway.configure().dataSource(url, "roomescape", "roomescape").schemas(schema)
			.locations("classpath:db/migration", "classpath:db/migration_v2").load().migrate();
		try (var connection = DriverManager.getConnection(url, "roomescape", "roomescape");
			var select = connection.prepareStatement("SELECT deleted_at FROM festival WHERE id = ?")) {
			select.setLong(1, eventId);
			try (var result = select.executeQuery()) {
				assertThat(result.next()).isTrue();
				assertThat(result.getTimestamp(1).toLocalDateTime().toString()).isEqualTo("2026-10-10T12:00");
			}
		}
		try (var connection = DriverManager.getConnection(url, "roomescape", "roomescape");
			var select = connection.prepareStatement("""
				SELECT version, script FROM flyway_schema_history
				WHERE version IN ('37', '38', '39', '40', '41') AND success = true ORDER BY installed_rank
				"""); var result = select.executeQuery()) {
			var applied = new java.util.ArrayList<String>();
			while (result.next()) applied.add(result.getString("script"));
			assertThat(applied).containsExactly(
				"V37__unify_festival_deletion.sql",
				"V38__add_festival_culture_info_columns.sql",
				"V39__drop_festival_legacy_api_only_columns.sql",
				"V40__create_deletion_history.sql",
				"V41__remove_legacy_festival_deletion_columns.sql");
		}
	}
}

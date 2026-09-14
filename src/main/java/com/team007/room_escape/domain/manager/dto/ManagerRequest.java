package com.team007.room_escape.domain.manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ManagerRequest {

	public record ManagerApply(

		@Size(max = 255)
		@NotBlank
		String organization,

		@Size(max = 255)
		@NotBlank
		String companyPhone,

		@NotBlank
		String reason
	) {}
}

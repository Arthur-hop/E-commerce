package ourpkg.customerService.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatCreationRequest {
	@NotNull
	private Integer shopId;
	@NotNull
	private Integer buyerId;
}

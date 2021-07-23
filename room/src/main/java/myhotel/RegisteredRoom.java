package myhotel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegisteredRoom extends AbstractEvent {

    private Long id;
    private Integer price;
    private Long hostId;
}
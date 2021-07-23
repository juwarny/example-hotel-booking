package myhotel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeletedRoom extends AbstractEvent {

    private Long id;
    private Integer price;
    private Long hostId;

}
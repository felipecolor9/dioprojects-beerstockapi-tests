package one.digitalinnovation.beerstock.service;

import one.digitalinnovation.beerstock.builder.BeerDTOBuilder;
import one.digitalinnovation.beerstock.dto.BeerDTO;
import one.digitalinnovation.beerstock.exception.BeerAlreadyRegisteredException;
import one.digitalinnovation.beerstock.exception.BeerNotFoundException;
import one.digitalinnovation.beerstock.exception.BeerStockExceededException;
import one.digitalinnovation.beerstock.mapper.BeerMapper;
import one.digitalinnovation.beerstock.repository.BeerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BeerServiceTest {

    private static final long INVALID_BEER_ID = 1L;

    @Mock
    private BeerRepository beerRepository;
    private BeerMapper beerMapper = BeerMapper.INSTANCE;

    @InjectMocks
    private BeerService beerService;

    @Test
    void whenBeerInformedThenItShouldBeCreated() throws BeerAlreadyRegisteredException {
        var expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        var expectedBeer = beerMapper.toModel(expectedBeerDTO);

        Mockito.when(beerRepository.findByName(expectedBeerDTO.getName())).thenReturn(Optional.empty());
        Mockito.when(beerRepository.save(expectedBeer)).thenReturn(expectedBeer);

        var createdBeerDTO = beerService.createBeer(expectedBeerDTO);

        // Default
        // assertEquals(expectedBeerDTO.getId(), createdBeerDTO.getId());
        // assertEquals(expectedBeerDTO.getName(), createdBeerDTO.getName());
        // assertEquals(expectedBeerDTO.getQuantity(), createdBeerDTO.getQuantity());

        // With Hamcrest
        assertThat(createdBeerDTO.getId(), is(equalTo(expectedBeer.getId())));
        assertThat(createdBeerDTO.getName(), is(equalTo(expectedBeer.getName())));
        assertThat(createdBeerDTO.getQuantity(), is(equalTo(expectedBeer.getQuantity())));

        // Beer Quantity tests
        assertThat(createdBeerDTO.getQuantity(), is(greaterThan(0)));
        assertThat(createdBeerDTO.getQuantity(), is(lessThanOrEqualTo(expectedBeer.getMax())));
    }

    @Test
    void whenAlreadyRegisteredBeerInformedThenAnExceptionShouldBeThrown() throws BeerAlreadyRegisteredException {
        var expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        var duplicatedBeer = beerMapper.toModel(expectedBeerDTO);

        Mockito.when(beerRepository.findByName(expectedBeerDTO.getName())).thenReturn(Optional.of(duplicatedBeer));

        assertThrows(BeerAlreadyRegisteredException.class, () -> beerService.createBeer(expectedBeerDTO));
    }

    @Test
    void whenValidBeerNameIsGivenThenReturnBeer() throws BeerNotFoundException {
        var expectedFoundBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        var expectedFoundBeer = beerMapper.toModel(expectedFoundBeerDTO);

        when(beerRepository.findByName(expectedFoundBeer.getName())).thenReturn(Optional.of(expectedFoundBeer));

        assertThat(beerService.findByName(expectedFoundBeerDTO.getName()), is(equalTo(expectedFoundBeerDTO)));
    }

    @Test
    void whenRegisteredBeerNameNotFoundThenAnExceptionShouldBeThrown() throws BeerNotFoundException {
        var expectedFoundBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();

        when(beerRepository.findByName(expectedFoundBeerDTO.getName())).thenReturn(Optional.empty());

        assertThrows(BeerNotFoundException.class, () -> beerService.findByName(expectedFoundBeerDTO.getName()));
    }

    @Test
    void whenBeerListIsCalledThenReturnAList() {
        var expectedFoundBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        var expectedFoundBeer = beerMapper.toModel(expectedFoundBeerDTO);

        when(beerRepository.findAll()).thenReturn(Collections.singletonList(expectedFoundBeer));
        List<BeerDTO> foundBeerListDTO = beerService.listAll();

        assertThat(foundBeerListDTO, is(not(empty())));
        assertThat(foundBeerListDTO.get(0), is(equalTo(expectedFoundBeerDTO)));
    }

    @Test
    void whenBeerListIsEmptyThenReturnAEmptyList() {
        when(beerRepository.findAll()).thenReturn(Collections.emptyList());
        List<BeerDTO> foundBeerListDTO = beerService.listAll();

        assertThat(foundBeerListDTO, is(empty()));
    }

    @Test
    void whenExclusionIsCalledWithValidIdThenABeerShouldBeDeleted() throws BeerNotFoundException {
        var expectedDeletedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        var expectedDeletedBeer = beerMapper.toModel(expectedDeletedBeerDTO);

        when(beerRepository.findById(expectedDeletedBeer.getId())).thenReturn(Optional.of(expectedDeletedBeer));
        doNothing().when(beerRepository).deleteById(expectedDeletedBeerDTO.getId());

        beerService.deleteById(expectedDeletedBeerDTO.getId());
        verify(beerRepository, times(1)).findById(expectedDeletedBeerDTO.getId());
        verify(beerRepository, times(1)).deleteById(expectedDeletedBeerDTO.getId());
    }

    @Test
    void whenIncrementIsCalledThenIncrementBeerStock() throws BeerNotFoundException, BeerStockExceededException {
        var expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        var expectedBeer = beerMapper.toModel(expectedBeerDTO);

        when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedBeer));
        when(beerRepository.save(expectedBeer)).thenReturn(expectedBeer);

        var quantityToIncrement = 10;
        var expectedQuantityAfterIncrement = expectedBeerDTO.getQuantity() + quantityToIncrement;
        var incrementedBeerDTO = beerService.increment(expectedBeerDTO.getId(), quantityToIncrement);

        assertThat(expectedQuantityAfterIncrement, is(equalTo(incrementedBeerDTO.getQuantity())));
        assertThat(expectedQuantityAfterIncrement, is(lessThan(expectedBeerDTO.getMax())));
    }

    @Test
    void whenBeerIncrementExceedTheMaximumAllowedThenAnExceptionShouldBeThrown() throws BeerStockExceededException {
        var expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        var expectedBeer = beerMapper.toModel(expectedBeerDTO);

        when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedBeer));
        int quantityToIncrement = 500;

        assertThrows(BeerStockExceededException.class, () -> beerService.increment( expectedBeerDTO.getId(), quantityToIncrement));
    }

    @Test
    void whenBeerIncrementAfterSumExceedTheMaximumAllowedThenAnExceptionShouldBeThrown() throws BeerStockExceededException {
        var expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        var expectedBeer = beerMapper.toModel(expectedBeerDTO);

        when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedBeer));
        int quantityToIncrement = 45;

        assertThrows(BeerStockExceededException.class, () -> beerService.increment( expectedBeerDTO.getId(), quantityToIncrement));
    }

    @Test
    void whenIncrementIsCalledWithInvalidIdThenAnExceptionShouldBeThrown() {
        int quantityToIncrement = 10;

        when(beerRepository.findById(INVALID_BEER_ID)).thenReturn(Optional.empty());

        assertThrows(BeerNotFoundException.class, () -> beerService.increment(INVALID_BEER_ID, quantityToIncrement));
    }
}

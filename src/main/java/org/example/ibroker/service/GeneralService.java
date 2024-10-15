package org.example.ibroker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ibroker.client.TelegramFeignClient;
import org.example.ibroker.dto.request.GeneralRequestDto;
import org.example.ibroker.dto.response.GeneralResponseDto;
import org.example.ibroker.entity.GeneralSearch;
import org.example.ibroker.repository.GeneralRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.modelmapper.ModelMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeneralService {

    private final GeneralRepository generalRepository;
    private final ModelMapper modelMapper;
    private final TelegramFeignClient telegramFeignClient;
    private final GeminiService geminiService;


    public String saveSearchDetails(GeneralRequestDto generalRequestDto) {
        GeneralSearch generalSearch = modelMapper.map(generalRequestDto, GeneralSearch.class);
        generalSearch.setSearchDate(LocalDateTime.now());
        generalRepository.save(generalSearch);
        return "Saved";
    }

    public List<GeneralResponseDto> getSearchDetails() {
        return generalRepository
                .findAll()
                .stream()
                .map(d -> modelMapper.map(d, GeneralResponseDto.class))
                .toList();
    }

    public String deleteSearchDetails(Long chatId) {
        generalRepository.deleteById(chatId);
        return "Deleted";
    }

    public static boolean isTimeAfter(String timeString, LocalDateTime dateTime) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime parsedTime = LocalTime.parse(timeString, timeFormatter);
        LocalTime timeFromDateTime = dateTime.toLocalTime();
        return parsedTime.isAfter(timeFromDateTime);
    }


    @Scheduled(fixedDelay = 300000)
    public void getFilters() throws IOException {
        List<GeneralSearch> searchDetails = generalRepository.findAll();
        for (GeneralSearch search : searchDetails) {
            Document doc = Jsoup.connect(search.getGeneralUrl()).get();
            Elements elements = doc.getElementsByClass("items-i");
            for (Element element : elements) {
                Element broker = element.getElementsByClass("products-label").first();
                if (broker == null || search.getAgent() == 1) {
                    Element time = element.getElementsByClass("city_when").first();
                    assert time != null;
                    String timeText = time.text();
                    if (timeText.contains("bugün")) {
                        String second = timeText.split(",")[1].trim();
                        String result = second.split(" ")[1];
                        if (isTimeAfter(result, search.getSearchDate())) {
                            Elements elementsByAttribute = element.getElementsByAttribute("href");
                            String link = elementsByAttribute.attr("abs:href");
                            Document doc2 = Jsoup.connect(link).get();
                            Element description = doc2.getElementsByClass("product-description__content").first();
                            if (search.getKeyword() == null || geminiService.analyze(search.getKeyword(), description.text())) {
                                telegramFeignClient.sendMessage(search.getChatId(), "Yeni elan: \n" + link);
                                search.setSearchDate(LocalDateTime.now().plusMinutes(1));
                            }
                        }
                    }
                }

            }
        }
    }



////    @Scheduled(fixedDelay = 6000)
//    public void testFilters() throws IOException {
//        Document doc = Jsoup.connect("https://bina.az").get();
//        Elements elements = doc.getElementsByClass("items-i");
//        for (Element element : elements) {
//
//
//            Elements elementsByAttribute = element.getElementsByAttribute("href");
//            String links = elementsByAttribute.attr("abs:href");
////            System.out.println(links);
//            Document doc2 = Jsoup.connect(links).get();
//
//
////            Element description = doc2.getElementsByClass("product-description__content").first();
//
////            assert description != null;
////            String string = description.toString();
//            String keyword = "tecili,təcili";
////            System.out.println(geminiService.analyze(keyword, description.text()));
//            Element time = element.getElementsByClass("city_when").first();
//            assert time != null;
//            String timeText = time.text();
//            if (timeText.contains("bugün")) {
//                String second = timeText.split(",")[1].trim();
//                String result = second.split(" ")[1];
//
//
//                boolean res =    isTimeAfter(result, LocalDateTime.now().plusMinutes(1));
//
//
//                System.out.println("Res : "  + res);
//
////            GeneralRequestDto generalRequestDto = new GeneralRequestDto();
////            System.out.println(processUSerRequest(generalRequestDto, element));
////            System.out.println(description);
////            GeneralRequestDto generalRequestDto2 = new GeneralRequestDto();
////            generalRequestDto2.setGeneralUrl(links);
////            generalRequestDto2.setDescription(string);
////            System.out.println(processUSerRequest(generalRequestDto2));
//
////            Document doc2 = Jsoup.connect(links).get();
//
//
////            Element time = element.getElementsByClass("city_when").first();
//
////                String link = element.getElementsByClass("item_link").first().attr("href");
////                String linkR = link.substring(0, 13);
////                Element broker = element.getElementsByClass("products-label").first();
////            Element description = element.getElementsByClass("product-description__content").first();
////            System.out.println(description);
//
//
////
////
////
////
////
////        }
//
//            }
//        }
//    }
}
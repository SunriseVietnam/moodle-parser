package com.polytech.moodleparser.parser;

import com.polytech.moodleparser.docxConfig.Question;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class XMLParser {
    public static List<Question> collectXMLData(String inputXML) {

        List<Question> questionsInfo = new ArrayList<>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document = builder.parse(new InputSource(new StringReader(inputXML)));

            document.getDocumentElement().normalize();

            NodeList questionList = document.getElementsByTagName("question");
            for (int i = 0; i < questionList.getLength(); i++) {

                Node question = questionList.item(i);

                if (question.getNodeType() == Node.ELEMENT_NODE) {

                    Element questionElement = (Element) question;
                    String questionType = questionElement.getAttribute("type");
                    String questionText = "";
                    ArrayList<String> answers = new ArrayList<>();

                    NodeList questionDetails = question.getChildNodes();
                    for (int j = 0; j < questionDetails.getLength(); j++) {

                        Node detail = questionDetails.item(j);
                        if (detail.getNodeType() == Node.ELEMENT_NODE) {
                            Element detailElement = (Element) detail;

                            if (Objects.equals(detailElement.getTagName(), "questiontext")) {
                                if (detailElement.getTextContent() != null) {
                                    questionText = detailElement.getTextContent();
                                }
                            }

                            if (Objects.equals(detailElement.getTagName(), "answer") || Objects.equals(detailElement.getTagName(), "dragbox")
                                    || Objects.equals(detailElement.getTagName(), "selectoption")) {

                                if (detailElement.getTextContent() != null) {
                                    String currentText = detailElement.getTextContent();
                                    if (detailElement.getTagName().equals("answer")) {
                                        String isCorrect = String.valueOf((!detailElement.getAttribute("fraction").equals("0")));
                                        if (isCorrect.equals("true")) {
                                            answers.add(currentText + isCorrect);
                                        } else {
                                            answers.add(currentText);
                                        }
                                    } else {
                                        answers.add(currentText);
                                    }
                                }
                            }
                        }
                    }
                    Question qstn = normalizeXMLData(questionType, answers, questionText);
                    boolean isDuplicate = false;
                    for(Question q: questionsInfo){
                        if(q.getText().equals(qstn.getText())){
                            isDuplicate = true;
                            break;
                        }
                    }
                    if(!isDuplicate &&
                            !qstn.getText().equals("") &&
                            !qstn.getType().equals("") &&
                            qstn.getAnswers()!=null) {
                        questionsInfo.add(qstn);
                    }
                }
            }

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            throw new RuntimeException(e);

        } catch (IOException | SAXException e) {
            throw new RuntimeException(e);
        }
        System.out.println(questionsInfo);
        return questionsInfo;
    }

    /**
     * Нормализует ответы
     * @param questionType - тип вопроса
     * @param answers - ответы
     * @param questionText - формулировка
     * @return Question
     */
    public static Question normalizeXMLData(String questionType, ArrayList<String> answers, String questionText) {
        int count = 0;
        Question normalizedAnswers = new Question();
        String newQuestionText = questionText.replaceAll("<[^>]*>", "").trim(); //тупо все теги убирает, это тупо, но я пока ничего лучше не придумал В(
        ArrayList<String> newAnswers = new ArrayList<>();
        StringBuilder answer = new StringBuilder();

        if (!Objects.equals(questionType, "essay")) {
            for (int i = 0; i < answers.toArray().length; i++) {
                count ++;

                String currentAnswer = answers.toArray()[i].toString().replaceAll("<[^>]*>", "").trim();
                String[] splitted = (currentAnswer.split("\n"));
                for (int j = 0; j < splitted.length; j++) {

                    currentAnswer = splitted[j].trim();
                    if (!currentAnswer.equals("")) {
                        switch (questionType) {
                            case "multichoice", "multichoiceset" -> {
                                if (j == 0) {
                                    answer = new StringBuilder(currentAnswer);
                                }
                                if (j == splitted.length - 1 && splitted[splitted.length - 1].trim().equals("true") && !Objects.equals(questionType,"ddwtos")) {
                                    answer.append(" - Правильный ответ");
                                }
                            }
                            case "ddwtos", "shortanswer", "numerical" -> {
                                if (j == 0) {
                                    answer = new StringBuilder(currentAnswer);
                                }
                            }
                            case "gapselect" -> {
                                String[] string = newQuestionText.split("");
                                if (j == 0) {
                                    answer = new StringBuilder(currentAnswer);
                                    for (int k = 0; k <= string.length; k++) {
                                        int index = -1;
                                        if (string[k].equals("[") && string[k + 1] != null && string[k + 1].equals("[") && string[k + 3] != null && string[k + 3].equals("]")
                                                && string[k + 4] != null && string[k + 4].equals("]")) {
                                            try {
                                                index = Integer.parseInt(string[k + 2]);
                                            } catch (NumberFormatException e) {
                                                throw new RuntimeException(e);
                                            }
                                            if (index > -1 && count == index) {
                                                answer = new StringBuilder(currentAnswer + " - Правильный ответ");
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                newAnswers.add(answer.toString());
                normalizedAnswers = new Question(newQuestionText,questionType, newAnswers);
            }
        } else {
            normalizedAnswers = new Question(newQuestionText, questionType, answers);
        }
        return normalizedAnswers;
    }
}
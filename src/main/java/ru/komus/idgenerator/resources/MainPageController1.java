package ru.komus.idgenerator.resources;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class MainPageController1
{
    @Value("${git.commit.committer.time}")
    private String committerTime;
    @Value("${git.commit.id}")
    private String commitId;
    @Value("${git.commit.message.full}")
    private String commitMessageFull;
    @Value("${git.commit.user.name}")
    private String commitUserName;


    @GetMapping
    public String getMainPage(Model model)
    {
        String gitInfo = String.format("Last commit was %s, hash %s, by %s, with message %s.", committerTime, commitId, commitUserName, commitMessageFull);

        model.addAttribute("gitInfo", gitInfo);
        return "index";
    }

}

package com.ntcoverage.seed

data class StatementTranslationEntry(
    val fatherNormalizedName: String,
    val sourceReference: String,
    val locale: String,
    val statementText: String
)

object TextualStatementTranslationsSeedData {

    val entries: List<StatementTranslationEntry> = listOf(

        // ==================== PORTUGUESE (pt) ====================

        // === IRENAEUS OF LYON ===
        StatementTranslationEntry("irenaeus_of_lyon", "3.1.1", "pt",
            "Mateus também publicou um Evangelho escrito entre os hebreus em seu próprio dialeto, enquanto Pedro e Paulo pregavam em Roma e lançavam os fundamentos da Igreja."),
        StatementTranslationEntry("irenaeus_of_lyon", "3.11.8", "pt",
            "Não é possível que os Evangelhos sejam mais ou menos em número do que são. Pois, visto que há quatro regiões do mundo em que vivemos e quatro ventos principais, a Igreja está espalhada por todo o mundo, e o pilar e fundamento da Igreja é o Evangelho e o espírito de vida; é apropriado que ela tenha quatro pilares."),
        StatementTranslationEntry("irenaeus_of_lyon", "5.30.1", "pt",
            "O número 666 é encontrado em todas as cópias mais aprovadas e antigas, e aqueles homens que viram João face a face dão testemunho disso."),
        StatementTranslationEntry("irenaeus_of_lyon", "1.27.2", "pt",
            "Marcião mutilou o Evangelho segundo Lucas, removendo todas as profecias sobre o Senhor e grande parte do ensino dos discursos do Senhor."),

        // === ORIGEN ===
        StatementTranslationEntry("origen", "15.14", "pt",
            "As diferenças entre os manuscritos tornaram-se grandes, seja pela negligência de alguns copistas ou pela audácia perversa de outros; eles ou negligenciam verificar o que transcreveram, ou, no processo de verificação, fazem adições ou supressões como lhes apraz."),
        StatementTranslationEntry("origen", "6.25.4", "pt",
            "Como aprendi pela tradição a respeito dos quatro Evangelhos, os únicos incontestados na Igreja de Deus sob o céu: o primeiro escrito foi por Mateus, que foi outrora cobrador de impostos mas depois apóstolo de Jesus Cristo, publicado para os crentes de origem judaica, composto em língua hebraica."),
        StatementTranslationEntry("origen", "6.25.8", "pt",
            "Paulo não escreveu a todas as igrejas que havia instruído; e mesmo àquelas a que escreveu enviou apenas poucas linhas. E Pedro, sobre quem a Igreja de Cristo é edificada, deixou uma epístola reconhecida; possivelmente também uma segunda, mas isso é duvidoso."),
        StatementTranslationEntry("origen", "2.27", "pt",
            "Hoje é evidente que há muitas diferenças entre os manuscritos, seja pelo descuido dos escribas, ou pela audácia perversa de algumas pessoas em corrigir o texto, ou pelo fato de que há aqueles que adicionam ou suprimem como acham conveniente ao fazer correções."),
        StatementTranslationEntry("origen", "1.Preface.8", "pt",
            "As Escrituras foram compostas pelo Espírito de Deus e têm um significado, não apenas o que é aparente à primeira vista, mas também outro, que escapa à percepção da maioria. Pois as palavras escritas são formas de certos mistérios e imagens de coisas divinas."),

        // === EUSEBIUS OF CAESAREA ===
        StatementTranslationEntry("eusebius_of_caesarea", "4.36", "pt",
            "Constantino ordenou que cinquenta cópias das Sagradas Escrituras fossem escritas em pergaminho preparado, de maneira legível e em formato portátil conveniente, por transcritores profissionais totalmente versados em sua arte."),
        StatementTranslationEntry("eusebius_of_caesarea", "3.25.1-4", "pt",
            "Entre os livros aceitos deve ser colocado o santo quaternário dos Evangelhos; a estes seguem-se os Atos dos Apóstolos. Depois devem ser contadas as epístolas de Paulo. Após elas, a primeira epístola de João e igualmente a primeira epístola de Pedro devem ser mantidas. Depois destas, se realmente parecer apropriado, o Apocalipse de João."),
        StatementTranslationEntry("eusebius_of_caesarea", "3.25.3", "pt",
            "Entre os escritos disputados, que no entanto são reconhecidos por muitos, existem a chamada epístola de Tiago e a de Judas, também a segunda epístola de Pedro, e aquelas chamadas segunda e terceira de João, sejam elas do evangelista ou de outra pessoa com o mesmo nome."),
        StatementTranslationEntry("eusebius_of_caesarea", "3.24.6", "pt",
            "Mateus havia começado pregando aos hebreus e, quando estava prestes a ir a outros também, confiou seu próprio Evangelho à escrita em sua língua nativa, compensando assim aqueles que era obrigado a deixar pela perda de sua presença."),
        StatementTranslationEntry("eusebius_of_caesarea", "3.39.17 (commentary tradition)", "pt",
            "A passagem sobre a mulher apanhada em adultério não é encontrada nas cópias mais precisas e não é reconhecida pelos mais estimados escritores eclesiásticos entre os antigos."),

        // === JEROME ===
        StatementTranslationEntry("jerome", "Ep. 27.1 (ad Marcellam)", "pt",
            "Não sou tão estúpido a ponto de pensar que qualquer das palavras do Senhor precisa de correção ou não é divinamente inspirada; mas os manuscritos latinos das Escrituras provam-se defeituosos pelas variações que todos exibem, e meu objetivo tem sido restaurá-los à forma do original grego."),
        StatementTranslationEntry("jerome", "Preface to the Vulgate Gospels, §1", "pt",
            "Vocês me compelem a fazer uma nova obra a partir de uma antiga, e depois de tantas cópias das Escrituras terem sido espalhadas pelo mundo, a sentar em julgamento e decidir quais delas concordam com o original grego."),
        StatementTranslationEntry("jerome", "Preface to the Vulgate Gospels, §2", "pt",
            "Os textos latinos são tão numerosos quanto as cópias. Cada pessoa adicionou ou alterou o que achou conveniente, e há quase tantas leituras quanto manuscritos."),
        StatementTranslationEntry("jerome", "Preface to the Vulgate Gospels, §3", "pt",
            "Usei como base de minha correção os antigos códices gregos. Comparei-os com aqueles que chamamos pelos nomes de Luciano e Hesíquio, e que a falsa erudição dos homens perverteu."),
        StatementTranslationEntry("jerome", "2.17", "pt",
            "A passagem sobre a mulher apanhada em adultério, no Evangelho segundo João, é encontrada em muitas cópias, tanto gregas quanto latinas."),

        // === AUGUSTINE ===
        StatementTranslationEntry("augustine_of_hippo", "Ep. 82.1.3 (ad Hieronymum)", "pt",
            "Aprendi a conceder este respeito e honra somente aos livros canônicos das Escrituras: somente destes creio firmemente que os autores estavam completamente livres de erro. E se nestes escritos me vejo perplexo diante de algo que me parece oposto à verdade, não hesito em supor que ou o manuscrito está defeituoso, ou o tradutor não captou o significado, ou eu mesmo falhei em compreender."),
        StatementTranslationEntry("augustine_of_hippo", "2.15.22", "pt",
            "Entre as traduções, a Ítala deve ser preferida às demais, pois se mantém mais próxima das palavras sem prejuízo da clareza de expressão. E ao corrigir qualquer versão latina, as versões gregas devem ser consultadas."),
        StatementTranslationEntry("augustine_of_hippo", "2.14.21", "pt",
            "Quando encontramos variações nas cópias das Sagradas Escrituras, ou suspeitamos que uma passagem esteja corrompida, devemos recorrer àqueles manuscritos que são os mais antigos e em maior concordância, e especialmente àqueles escritos na língua original."),
        StatementTranslationEntry("augustine_of_hippo", "2.8.12", "pt",
            "Em matéria de Escrituras canônicas, deve-se seguir a autoridade da maioria das igrejas católicas, entre as quais estão aquelas que mereceram ter sedes apostólicas e receber epístolas."),

        // === ATHANASIUS ===
        StatementTranslationEntry("athanasius_of_alexandria", "Festal Letter 39, §1", "pt",
            "Estas são as fontes da salvação, para que os sedentos possam se satisfazer com as palavras vivas que contêm. Somente nestas é proclamada a doutrina da piedade. Que ninguém acrescente a elas, nem tire algo delas."),
        StatementTranslationEntry("athanasius_of_alexandria", "Festal Letter 39, §2", "pt",
            "Além disso, há outros livros além dos anteriores, que não estão incluídos no Cânon, mas foram designados pelos Pais para serem lidos por aqueles que se juntam a nós recentemente e desejam instrução: a Sabedoria de Salomão, e a Sabedoria de Siraque, e Ester, e Judite, e Tobias, e a Didaquê, e o Pastor."),
        StatementTranslationEntry("athanasius_of_alexandria", "1.1", "pt",
            "As santas Escrituras inspiradas por Deus são autossuficientes para a pregação da verdade."),

        // === TERTULLIAN ===
        StatementTranslationEntry("tertullian", "36", "pt",
            "Vinde, vós que desejais satisfazer uma curiosidade melhor, se a aplicardes ao negócio de vossa salvação, percorrei as igrejas apostólicas, nas quais os próprios tronos dos apóstolos ainda são preeminentes em seus lugares, nas quais seus próprios escritos autênticos são lidos."),
        StatementTranslationEntry("tertullian", "17", "pt",
            "Sei que o escrito chamado Atos de Paulo e Tecla, sob o nome de Paulo, foi forjado por um presbítero da Ásia. Ele foi condenado e, tendo confessado que o fez por amor a Paulo, foi removido de seu cargo."),
        StatementTranslationEntry("tertullian", "38", "pt",
            "Marcião expressamente e abertamente usou a faca, não a caneta, pois fez tal excisão das Escrituras como convinha ao seu próprio assunto."),
        StatementTranslationEntry("tertullian", "4.2", "pt",
            "Estabelecemos como nossa primeira posição que o Testamento evangélico tem apóstolos como seus autores, aos quais foi designado pelo próprio Senhor este ofício de publicar o evangelho."),

        // === CLEMENT OF ALEXANDRIA ===
        StatementTranslationEntry("clement_of_alexandria", "1.1.11", "pt",
            "Os benditos presbíteros, que preservaram a verdadeira tradição do bendito ensino diretamente dos santos apóstolos Pedro, Tiago, João e Paulo, os filhos recebendo do pai, vieram pela vontade de Deus a nós para depositar aquelas sementes ancestrais e apostólicas."),
        StatementTranslationEntry("clement_of_alexandria", "6.14.7", "pt",
            "Novamente, nos mesmos livros, Clemente dá a tradição dos mais antigos presbíteros quanto à ordem dos Evangelhos da seguinte maneira. Os Evangelhos contendo as genealogias, diz ele, foram escritos primeiro. Mas por último, João, percebendo que os fatos corporais haviam sido esclarecidos no Evangelho, por insistência de seus amigos e inspirado pelo Espírito, compôs um Evangelho espiritual."),

        // === CYPRIAN ===
        StatementTranslationEntry("cyprian_of_carthage", "Ep. 74.2", "pt",
            "De onde vem essa tradição? Descende ela da autoridade do Senhor e do Evangelho, ou vem dos mandamentos e das epístolas dos apóstolos? Pois que aquelas coisas que estão escritas devem ser feitas, Deus testemunha e adverte."),

        // === PAPIAS ===
        StatementTranslationEntry("papias_of_hierapolis", "3.39.15", "pt",
            "Marcos, tendo se tornado o intérprete de Pedro, escreveu com precisão, embora não em ordem, tudo o que se lembrava das coisas ditas ou feitas por Cristo. Pois ele nem ouviu o Senhor nem o acompanhou, mas depois, como eu disse, acompanhou Pedro."),
        StatementTranslationEntry("papias_of_hierapolis", "3.39.16", "pt",
            "Mateus compilou os oráculos na língua hebraica, e cada um os interpretou como melhor pôde."),

        // ==================== SPANISH (es) ====================

        // === IRENAEUS OF LYON ===
        StatementTranslationEntry("irenaeus_of_lyon", "3.1.1", "es",
            "Mateo también publicó un Evangelio escrito entre los hebreos en su propio dialecto, mientras Pedro y Pablo predicaban en Roma y sentaban los fundamentos de la Iglesia."),
        StatementTranslationEntry("irenaeus_of_lyon", "3.11.8", "es",
            "No es posible que los Evangelios sean más o menos en número de lo que son. Pues, dado que hay cuatro regiones del mundo en que vivimos y cuatro vientos principales, la Iglesia está esparcida por todo el mundo, y el pilar y fundamento de la Iglesia es el Evangelio y el espíritu de vida; es apropiado que tenga cuatro pilares."),
        StatementTranslationEntry("irenaeus_of_lyon", "5.30.1", "es",
            "El número 666 se encuentra en todas las copias más aprobadas y antiguas, y aquellos hombres que vieron a Juan cara a cara dan testimonio de ello."),
        StatementTranslationEntry("irenaeus_of_lyon", "1.27.2", "es",
            "Marción mutiló el Evangelio según Lucas, eliminando todas las profecías sobre el Señor y gran parte de la enseñanza de los discursos del Señor."),

        // === ORIGEN ===
        StatementTranslationEntry("origen", "15.14", "es",
            "Las diferencias entre los manuscritos se han hecho grandes, sea por la negligencia de algunos copistas o por la audacia perversa de otros; o descuidan verificar lo que han transcrito, o en el proceso de verificación hacen adiciones o supresiones como les place."),
        StatementTranslationEntry("origen", "6.25.4", "es",
            "Como aprendí por tradición respecto a los cuatro Evangelios, los únicos indiscutidos en la Iglesia de Dios bajo el cielo: el primero escrito fue por Mateo, que fue una vez recaudador de impuestos pero después apóstol de Jesucristo, publicado para los creyentes de origen judío, compuesto en lengua hebrea."),
        StatementTranslationEntry("origen", "6.25.8", "es",
            "Pablo no escribió a todas las iglesias que había instruido; e incluso a aquellas a las que escribió envió solo pocas líneas. Y Pedro, sobre quien la Iglesia de Cristo está edificada, dejó una epístola reconocida; posiblemente también una segunda, pero esto es dudoso."),
        StatementTranslationEntry("origen", "2.27", "es",
            "Hoy es evidente que hay muchas diferencias entre los manuscritos, sea por el descuido de los escribas, o la audacia perversa de algunas personas en corregir el texto, o por el hecho de que hay quienes añaden o suprimen como les parece al hacer correcciones."),
        StatementTranslationEntry("origen", "1.Preface.8", "es",
            "Las Escrituras fueron compuestas por el Espíritu de Dios y tienen un significado, no solo el que es aparente a primera vista, sino también otro que escapa a la percepción de la mayoría. Pues las palabras escritas son formas de ciertos misterios e imágenes de cosas divinas."),

        // === EUSEBIUS OF CAESAREA ===
        StatementTranslationEntry("eusebius_of_caesarea", "4.36", "es",
            "Constantino ordenó que cincuenta copias de las Sagradas Escrituras fueran escritas en pergamino preparado, de manera legible y en formato portátil conveniente, por transcriptores profesionales ampliamente versados en su arte."),
        StatementTranslationEntry("eusebius_of_caesarea", "3.25.1-4", "es",
            "Entre los libros aceptados debe colocarse el santo cuaternión de los Evangelios; a estos les siguen los Hechos de los Apóstoles. Después deben contarse las epístolas de Pablo. Tras ellas, la primera epístola de Juan e igualmente la primera epístola de Pedro deben ser mantenidas. Después de estas, si realmente parece apropiado, el Apocalipsis de Juan."),
        StatementTranslationEntry("eusebius_of_caesarea", "3.25.3", "es",
            "Entre los escritos disputados, que sin embargo son reconocidos por muchos, existen la llamada epístola de Santiago y la de Judas, también la segunda epístola de Pedro, y aquellas llamadas segunda y tercera de Juan, sean del evangelista o de otra persona del mismo nombre."),
        StatementTranslationEntry("eusebius_of_caesarea", "3.24.6", "es",
            "Mateo había comenzado predicando a los hebreos, y cuando estaba a punto de ir a otros también, confió su propio Evangelio a la escritura en su lengua nativa, compensando así a aquellos que estaba obligado a dejar por la pérdida de su presencia."),
        StatementTranslationEntry("eusebius_of_caesarea", "3.39.17 (commentary tradition)", "es",
            "El pasaje sobre la mujer sorprendida en adulterio no se encuentra en las copias más precisas y no es reconocido por los más estimados escritores eclesiásticos entre los antiguos."),

        // === JEROME ===
        StatementTranslationEntry("jerome", "Ep. 27.1 (ad Marcellam)", "es",
            "No soy tan estúpido como para pensar que alguna de las palabras del Señor necesita corrección o no es divinamente inspirada; pero los manuscritos latinos de las Escrituras se prueban defectuosos por las variaciones que todos exhiben, y mi objetivo ha sido restaurarlos a la forma del original griego."),
        StatementTranslationEntry("jerome", "Preface to the Vulgate Gospels, §1", "es",
            "Me obligan a hacer una obra nueva a partir de una antigua, y después de que tantas copias de las Escrituras se han esparcido por el mundo, a sentarme en juicio y decidir cuáles de ellas concuerdan con el original griego."),
        StatementTranslationEntry("jerome", "Preface to the Vulgate Gospels, §2", "es",
            "Los textos latinos son tan numerosos como las copias. Cada persona ha añadido o cambiado lo que le pareció conveniente, y hay casi tantas lecturas como manuscritos."),
        StatementTranslationEntry("jerome", "Preface to the Vulgate Gospels, §3", "es",
            "He usado como base de mi corrección los antiguos códices griegos. Los he comparado con aquellos que llamamos por los nombres de Luciano y Hesiquio, y que la falsa erudición de los hombres ha pervertido."),
        StatementTranslationEntry("jerome", "2.17", "es",
            "El pasaje sobre la mujer sorprendida en adulterio, en el Evangelio según Juan, se encuentra en muchas copias, tanto griegas como latinas."),

        // === AUGUSTINE ===
        StatementTranslationEntry("augustine_of_hippo", "Ep. 82.1.3 (ad Hieronymum)", "es",
            "He aprendido a conceder este respeto y honor solo a los libros canónicos de las Escrituras: solo de estos creo firmemente que los autores estaban completamente libres de error. Y si en estos escritos me veo perplejo ante algo que me parece opuesto a la verdad, no dudo en suponer que o el manuscrito es defectuoso, o el traductor no captó el significado, o yo mismo he fallado en comprender."),
        StatementTranslationEntry("augustine_of_hippo", "2.15.22", "es",
            "Entre las traducciones, la Ítala debe ser preferida a las demás, pues se mantiene más cercana a las palabras sin perjuicio de la claridad de expresión. Y al corregir cualquier versión latina, las versiones griegas deben ser consultadas."),
        StatementTranslationEntry("augustine_of_hippo", "2.14.21", "es",
            "Cuando encontramos variaciones en las copias de las Sagradas Escrituras, o sospechamos que un pasaje está corrupto, debemos recurrir a aquellos manuscritos que son los más antiguos y en mayor concordancia, y especialmente a aquellos escritos en la lengua original."),
        StatementTranslationEntry("augustine_of_hippo", "2.8.12", "es",
            "En materia de Escrituras canónicas, debe seguirse la autoridad de la mayoría de las iglesias católicas, entre las cuales están aquellas que han merecido tener sedes apostólicas y recibir epístolas."),

        // === ATHANASIUS ===
        StatementTranslationEntry("athanasius_of_alexandria", "Festal Letter 39, §1", "es",
            "Estas son las fuentes de la salvación, para que los sedientos puedan satisfacerse con las palabras vivas que contienen. Solo en estas se proclama la doctrina de la piedad. Que nadie añada a ellas, ni quite algo de ellas."),
        StatementTranslationEntry("athanasius_of_alexandria", "Festal Letter 39, §2", "es",
            "Además, hay otros libros aparte de los anteriores, que no están incluidos en el Canon, pero fueron designados por los Padres para ser leídos por quienes se unen a nosotros recientemente y desean instrucción: la Sabiduría de Salomón, y la Sabiduría de Sirácida, y Ester, y Judit, y Tobías, y la Didajé, y el Pastor."),
        StatementTranslationEntry("athanasius_of_alexandria", "1.1", "es",
            "Las santas Escrituras inspiradas por Dios son autosuficientes para la predicación de la verdad."),

        // === TERTULLIAN ===
        StatementTranslationEntry("tertullian", "36", "es",
            "Venid, vosotros que quisierais satisfacer una curiosidad mejor, si la aplicarais al negocio de vuestra salvación, recorred las iglesias apostólicas, en las cuales los propios tronos de los apóstoles aún son preeminentes en sus lugares, en las cuales sus propios escritos auténticos son leídos."),
        StatementTranslationEntry("tertullian", "17", "es",
            "Sé que el escrito llamado Hechos de Pablo y Tecla, bajo el nombre de Pablo, fue falsificado por un presbítero de Asia. Fue condenado y, habiendo confesado que lo hizo por amor a Pablo, fue removido de su cargo."),
        StatementTranslationEntry("tertullian", "38", "es",
            "Marción expresamente y abiertamente usó el cuchillo, no la pluma, pues hizo tal excisión de las Escrituras como convenía a su propio tema."),
        StatementTranslationEntry("tertullian", "4.2", "es",
            "Establecemos como nuestra primera posición que el Testamento evangélico tiene apóstoles como sus autores, a quienes fue asignado por el Señor mismo este oficio de publicar el evangelio."),

        // === CLEMENT OF ALEXANDRIA ===
        StatementTranslationEntry("clement_of_alexandria", "1.1.11", "es",
            "Los benditos presbíteros, que preservaron la verdadera tradición de la bendita enseñanza directamente de los santos apóstoles Pedro, Santiago, Juan y Pablo, los hijos recibiéndola del padre, vinieron por voluntad de Dios a nosotros para depositar aquellas semillas ancestrales y apostólicas."),
        StatementTranslationEntry("clement_of_alexandria", "6.14.7", "es",
            "Nuevamente, en los mismos libros, Clemente da la tradición de los más antiguos presbíteros en cuanto al orden de los Evangelios de la siguiente manera. Los Evangelios que contienen las genealogías, dice, fueron escritos primero. Pero último de todos, Juan, percibiendo que los hechos corporales habían sido aclarados en el Evangelio, por insistencia de sus amigos e inspirado por el Espíritu, compuso un Evangelio espiritual."),

        // === CYPRIAN ===
        StatementTranslationEntry("cyprian_of_carthage", "Ep. 74.2", "es",
            "¿De dónde viene esa tradición? ¿Desciende de la autoridad del Señor y del Evangelio, o viene de los mandamientos y las epístolas de los apóstoles? Pues que aquellas cosas que están escritas deben hacerse, Dios lo testifica y advierte."),

        // === PAPIAS ===
        StatementTranslationEntry("papias_of_hierapolis", "3.39.15", "es",
            "Marcos, habiéndose convertido en intérprete de Pedro, escribió con precisión, aunque no en orden, todo lo que recordaba de las cosas dichas o hechas por Cristo. Pues él ni oyó al Señor ni lo acompañó, sino que después, como dije, acompañó a Pedro."),
        StatementTranslationEntry("papias_of_hierapolis", "3.39.16", "es",
            "Mateo compiló los oráculos en lengua hebrea, y cada uno los interpretó como mejor pudo.")
    )
}

# iot-ktor-server
## Кейсы работы с запросами

### Кейс 1. Регистрация профиля.
* Кейс состоит из нескольких последовательных запросов *
* А именно: 
* 1. Пост запрос на сервер с указанием почты для отправления вериф кода *
* 2. Пост запрос на сервер для подтверждения указанного кода *
* 3. Пост запрос на регистрацию *

* Суть регистрации заключается в том, что пользователь в одном окне десктоп приложенияя вводит все необходимые данные для регистрации *
* Перед тем, как нажать кнопку зарегестрироваться, пользователь должен выслать код на почту *
* Примерное расположение *

<!doctype html>
<html lang="ru">
<head>
  <meta charset="utf-8" />
  <title></title> />
</head>
<body>
<form>
	<fieldset>
		<legend>Окно регистрации</legend>
		   <p><label for="name">Email <em> </em></label><input type="text" id="name"></p>
			<a href=#>Отправить код подтверждения</a>
			<p>Код потверждения: <label for="code"></label><input type="number" id="code"></p>
    		<p><label for="email">Login<em> </em></label><input type="email" id="email"></p>
			<p><label for="number">Number<em> </em></label><input type="number" id="number"></p>
			<p><label for="password">Password<em> </em></label><input type="password" id="password"></p>
			<a href=#>Зарегистрироваться</a>
	</fieldset>
</form>
</body>
</html>
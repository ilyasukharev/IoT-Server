# iot-ktor-server
## Работа с Redis - сервером на ubuntu.
### sudo service redis-server start
### redis-cli


## Кейсы работы с запросами

### Кейс 1. Регистрация профиля.
* Кейс состоит из нескольких последовательных запросов
* А именно: 
* Пост запрос на сервер с указанием почты для отправления вериф кода
* Пост запрос на сервер для подтверждения указанного кода
* Пост запрос на регистрацию

* Суть регистрации заключается в том, что пользователь в одном окне десктоп приложенияя вводит все необходимые данные для регистрации *
* Перед тем, как нажать кнопку зарегестрироваться, пользователь должен выслать код на почту *
* Примерное расположение *


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

* Запросы для обращения:
  1. POST http://localhost:8080/code/send
  2. POST http://localhost:8080/code/verify
  3. POST http://localhost:8080/account/register
* В этих 3-х запросах используется Json - Сериализация, а значит тип в заголовках (Headers) передаваемого запроса
* должен быть Content-Type: application/json. Как правило в постмане ":" - разделитель между двух колонок, т.е.
* Content-Type мы помещаем в одну колонку как название параметра, application/json - как значение в другую.

### Кейс 2. Восстановление пароля.
* Кейс состоит из нескольких последовательных запросов
* А именно:
* Пост запрос на сервер с указанием почты для отправления вериф кода
* Пост запрос на сервер для подтверждения указанного кода
* Пост запрос на Изменение пароля
* Пост запрос на внесение изменений в пароль

* Запросы для обращения:
  1. POST http://localhost:8080/code/send
  2. POST http://localhost:8080/code/verify
  3. POST http://localhost:8080/account/password/reset
  4. POST http://localhost:8080/account/change/password

* Здесь все то же самое, что и в примере выше, за исключением того, что в результате выполнения 3 запроса мы получаем токен, который должны использовать в 4 запросе.
* Для этого в постмане необходимо задать еще один параметр, а именно Authenticate: Bearer <token>. Authenticate - название параметра, Bearer <token> - значение.
* Пример: Authenticate: Bearer asksdmlaksd.asdasdasdasdasdasdasdasdasdasdads.asdasdasdasd

### Кейс 3. Вход
* Кейс состоит из одного запроса
* А именно:
* Пост запрос на вход

* Запросы для обращения:
  1. POST http://localhost:8080/account/login


### Тело Json запросов. Или то, что должен включать КАЖДЫЙ ПОСТ запрос.

* /account/register - 
  number: String
  email: String
  password: String

* !!! ЗДЕСЬ ТРИ ПОЛЯ NUMBER, EMAIL, PASSWORD. МЫ ДОЛЖНЫ ИХ ПЕРЕДАТЬ В КАЧЕСТВЕ ТЕЛА В ПОСТ ЗАПРОСЕ.

* /account/login -
  email: String
  password: String

* /account/password/reset -
 email: String

* /account/change/password -
  password: String

* /code/send
  email: String

* /code/verify -
  val email: String
  val code: Int


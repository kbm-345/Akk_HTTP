The server will start running at http://localhost:8080.

You can use tools like cURL or Postman to interact with the API endpoints:

1) **Insert:** Send a POST request to http://localhost:8080/insert with a CSV-formatted payload containing index, firstName, lastName, and email.

2) **Retrieve all:** Send a GET request to http://localhost:8080/allEmployee to get details of all Person entities.

3) **Retrieve by ID:** Send a GET request to http://localhost:8080/employee/{id} to get details of a specific Person entity.

4) **Update:** Send a PUT request to http://localhost:8080/employee/{id} with a CSV-formatted payload containing firstName, lastName, and email.

5) **Delete:** Send a DELETE request to http://localhost:8080/employee/{id} to delete a Person entity by ID.

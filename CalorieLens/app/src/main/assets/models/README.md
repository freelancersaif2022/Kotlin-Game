Place the on-device food classifier here as `food_model.tflite`.

The CI workflow downloads Google's AIY "vision classifier food V1" TensorFlow Lite model
(~2,000 dishes, with label metadata) into this folder before building. To build locally,
download it from Kaggle Models (google/aiy, variation `vision-classifier-food-v1`) and
save the `.tflite` file here with that exact name.
